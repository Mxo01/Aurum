package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotMapperTest {

	private static final Currency USER_CURRENCY = Currency.USD;
	private static final Currency ASSET_CURRENCY = Currency.EUR;
	private static final Currency ROW_CURRENCY = Currency.GBP;
	private static final Currency CHANGED_ASSET_CURRENCY = Currency.CHF;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 1, 31);
	private static final BigDecimal RATE_TO_USER_CURRENCY = new BigDecimal("1.5");
	private static final BigDecimal ORIGINAL_AMOUNT = new BigDecimal("100");
	private static final BigDecimal CONVERTED_AMOUNT = new BigDecimal("150");
	private static final BigDecimal STORED_RATE = new BigDecimal("2.0000");
	private static final BigDecimal AMOUNT_IN_BASE_CURRENCY = new BigDecimal("200");
	private static final int BASE_AMOUNT_SCALE = 4;
	private static final String ASSET_NOT_OWNED_MESSAGE = "This asset does not belong to you";
	private static final String USER_ID_FIELD = "userId";

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@InjectMocks
	private SnapshotMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Snapshot result = testSubject.toEntity((CreateSnapshotDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsAndResolvesAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), RATE_TO_USER_CURRENCY)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getReferenceDate()).isEqualTo(mockDto.getReferenceDate());
		assertThat(result.getAmountOriginalCurrency()).isEqualTo(mockDto.getAmountOriginalCurrency());
		assertThat(result.getExchangeRateToBase()).isEqualTo(mockDto.getExchangeRateToBase());
		assertThat(result.getAsset()).isEqualTo(mockAsset);
		assertThat(result.getSource()).isEqualTo(SnapshotSource.USER);
		assertThat(result.getCurrency()).isEqualTo(mockAsset.getOriginalCurrency());
	}

	@Test
	void toEntity_createDto_defaultsExchangeRateToOne_whenNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void toEntity_createDto_throwsNotFoundException_whenAssetNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_createDto_throwsAccessDeniedException_whenAssetBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(ASSET_NOT_OWNED_MESSAGE);
	}

	@Test
	void toEntity_createDto_leavesAssetNull_whenDtoHasNoAssetId() {
		// GIVEN
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), null)
			.create();

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, UUID.randomUUID());

		// THEN
		assertThat(result.getAsset()).isNull();
	}

	@Test
	void toEntity_createDto_rejectsANullUserId_beforeTheAssetIsLoaded() {
		// GIVEN
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage(USER_ID_FIELD);
		verifyNoInteractions(assetRepository);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN
		RateBook mockBook = book(USER_CURRENCY, List.of(), List.of());

		// WHEN
		SnapshotDTO result = testSubject.toDto(null, mockBook);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_convertsAmountAtTheSnapshotReferenceDate() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset, ASSET_CURRENCY);
		RateBook mockBook = stubBook(ASSET_CURRENCY, USER_CURRENCY, REFERENCE_DATE);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getId()).isEqualTo(mockSnapshot.getId());
		assertThat(result.getReferenceDate()).isEqualTo(REFERENCE_DATE);
		assertThat(result.getAmountOriginalCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
		assertThat(result.getCurrency()).isEqualTo(ASSET_CURRENCY);
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(RATE_TO_USER_CURRENCY);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(CONVERTED_AMOUNT);
		assertThat(result.getAssetId()).isEqualTo(mockAsset.getId());
		assertThat(result.getAssetName()).isEqualTo(mockAsset.getName());
	}

	@Test
	void toDto_readsThroughTheRowsOwnCurrency_whenTheAssetCurrencyChangedSince() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), CHANGED_ASSET_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset, ROW_CURRENCY);
		RateBook mockBook = stubBook(ROW_CURRENCY, USER_CURRENCY, REFERENCE_DATE);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getCurrency()).isEqualTo(ROW_CURRENCY);
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(RATE_TO_USER_CURRENCY);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(CONVERTED_AMOUNT);
	}

	@Test
	void toDto_convertsThroughTheAssetCurrency_whenALegacyRowCarriesNone() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset, null);
		RateBook mockBook = stubBook(ASSET_CURRENCY, USER_CURRENCY, REFERENCE_DATE);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getCurrency()).isEqualTo(ASSET_CURRENCY);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(CONVERTED_AMOUNT);
	}

	@ParameterizedTest(name = "toDto of a {0} row")
	@EnumSource(SnapshotSource.class)
	void toDto_carriesTheSourceInTheDtoAndItsJson(SnapshotSource source)
		throws JsonProcessingException {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), USER_CURRENCY)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.set(Select.field(Snapshot::getCurrency), USER_CURRENCY)
			.set(Select.field(Snapshot::getSource), source)
			.set(Select.field(Snapshot::getReferenceDate), REFERENCE_DATE)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), ORIGINAL_AMOUNT)
			.create();
		RateBook mockBook = book(USER_CURRENCY, List.of(USER_CURRENCY), List.of(REFERENCE_DATE));

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getSource()).isEqualTo(source);
		assertThat(new ObjectMapper().findAndRegisterModules().writeValueAsString(result)).contains(
			"\"source\":\"" + source.name() + "\""
		);
	}

	@Test
	void toDto_isIdentity_whenTheRowUsesTheUserCurrency() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset, USER_CURRENCY);
		RateBook mockBook = book(USER_CURRENCY, List.of(USER_CURRENCY), List.of(REFERENCE_DATE));

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getCurrency()).isEqualTo(USER_CURRENCY);
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
	}

	@Test
	void toDto_convertsInTheBookTarget_whenNeitherTheRowNorTheAssetCarriesACurrency() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), null)
			.create();
		Snapshot mockSnapshot = buildSnapshot(mockAsset, null);
		RateBook mockBook = book(USER_CURRENCY, List.of(), List.of());
		BigDecimal expectedAnalyticsBase = mockBook.convert(
			ORIGINAL_AMOUNT,
			mockBook.target(),
			REFERENCE_DATE
		);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getCurrency()).isEqualTo(mockBook.target());
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(expectedAnalyticsBase);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(ORIGINAL_AMOUNT);
		assertThat(result.getAssetId()).isEqualTo(mockAsset.getId());
		assertThat(result.getAssetName()).isEqualTo(mockAsset.getName());
	}

	@Test
	void toDto_omitsAssetDetails_whenTheRowHasNoAsset() {
		// GIVEN
		Snapshot mockSnapshot = buildSnapshot(null, ROW_CURRENCY);
		RateBook mockBook = stubBook(ROW_CURRENCY, USER_CURRENCY, REFERENCE_DATE);

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot, mockBook);

		// THEN
		assertThat(result.getAssetId()).isNull();
		assertThat(result.getAssetName()).isNull();
		assertThat(result.getCurrency()).isEqualTo(ROW_CURRENCY);
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(CONVERTED_AMOUNT);
	}

	@Test
	void getAmountInBaseCurrency_convertsTheAmountAtTheStoredRate() {
		// GIVEN
		Snapshot testSubject = buildSnapshot(null, ROW_CURRENCY);

		// WHEN
		BigDecimal expectedAmount = testSubject.getAmountInBaseCurrency();

		// THEN
		assertThat(expectedAmount).isEqualByComparingTo(AMOUNT_IN_BASE_CURRENCY);
		assertThat(expectedAmount.scale()).isEqualTo(BASE_AMOUNT_SCALE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("incompleteAmounts")
	void getAmountInBaseCurrency_returnsZero_whenTheRowCannotBeConverted(
		String scenario,
		BigDecimal amountOriginalCurrency,
		BigDecimal exchangeRateToBase
	) {
		// GIVEN
		Snapshot testSubject = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), amountOriginalCurrency)
			.set(Select.field(Snapshot::getExchangeRateToBase), exchangeRateToBase)
			.create();

		// WHEN
		BigDecimal expectedAmount = testSubject.getAmountInBaseCurrency();

		// THEN
		assertThat(expectedAmount).isEqualByComparingTo(BigDecimal.ZERO);
	}

	static Stream<Arguments> incompleteAmounts() {
		return Stream.of(
			Arguments.of("a row without amount", null, STORED_RATE),
			Arguments.of("a row without exchange rate", ORIGINAL_AMOUNT, null),
			Arguments.of("a row without amount and rate", null, null)
		);
	}

	private Snapshot buildSnapshot(Asset asset, Currency rowCurrency) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getCurrency), rowCurrency)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), ORIGINAL_AMOUNT)
			.set(Select.field(Snapshot::getReferenceDate), REFERENCE_DATE)
			.set(Select.field(Snapshot::getExchangeRateToBase), STORED_RATE)
			.create();
	}

	private RateBook stubBook(Currency source, Currency target, LocalDate date) {
		when(
			exchangeRateProvider.getRates(source.getValue(), target.getValue(), List.of(date))
		).thenReturn(Map.of(date, RATE_TO_USER_CURRENCY));
		return book(target, List.of(source), List.of(date));
	}

	private RateBook book(Currency target, List<Currency> sources, List<LocalDate> dates) {
		return new CurrencyConversionService(exchangeRateProvider).book(target, sources, dates);
	}
}
