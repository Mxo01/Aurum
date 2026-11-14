package com.backend.aurum.domain.asset.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotFacadeTest {

	private static final Currency USER_CURRENCY = Currency.USD;
	private static final Currency ASSET_CURRENCY = Currency.EUR;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 5, 31);
	private static final LocalDate PREVIOUS_REFERENCE_DATE = LocalDate.of(2026, 4, 30);
	private static final BigDecimal RATE_TO_USER_CURRENCY = new BigDecimal("1.5");
	private static final BigDecimal EXPLICIT_RATE = new BigDecimal("1.25");
	private static final BigDecimal AMOUNT = new BigDecimal("100");

	@Mock
	private SnapshotService snapshotService;

	@Mock
	private SnapshotMapper mapper;

	@Mock
	private SnapshotValidationService validationService;

	@Mock
	private AssetService assetService;

	@Mock
	private CurrencyConversionService conversionService;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private SnapshotFacade testSubject;

	@Test
	void getSnapshots_returnsDtosConvertedIntoTheUserCurrency() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = List.of(
			buildSnapshot(REFERENCE_DATE, ASSET_CURRENCY),
			buildSnapshot(PREVIOUS_REFERENCE_DATE, ASSET_CURRENCY)
		);
		List<LocalDate> stubbedDates = List.of(REFERENCE_DATE, PREVIOUS_REFERENCE_DATE);
		SnapshotDTO stubbedFirstDto = Instancio.create(SnapshotDTO.class);
		SnapshotDTO stubbedSecondDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, stubbedDates);
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0), book)).thenReturn(stubbedFirstDto);
		when(mapper.toDto(stubbedSnapshots.get(1), book)).thenReturn(stubbedSecondDto);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedFirstDto, stubbedSecondDto);
		assertThat(book.convert(AMOUNT, ASSET_CURRENCY, REFERENCE_DATE)).isEqualByComparingTo(
			AMOUNT.multiply(RATE_TO_USER_CURRENCY)
		);
	}

	@Test
	void getSnapshots_isIdentity_whenTheAssetUsesTheUserCurrency() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = List.of(buildSnapshot(REFERENCE_DATE, USER_CURRENCY));
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(USER_CURRENCY, USER_CURRENCY, List.of(REFERENCE_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0), book)).thenReturn(stubbedDto);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto);
		assertThat(book.convert(AMOUNT, USER_CURRENCY, REFERENCE_DATE)).isEqualByComparingTo(AMOUNT);
	}

	@Test
	void getSnapshots_throwsNotFoundException_whenUserIsMissing() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = List.of(buildSnapshot(REFERENCE_DATE, ASSET_CURRENCY));
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getSnapshots(mockAssetId, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void getAllSnapshots_returnsDtosConvertedIntoTheUserCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = List.of(
			buildSnapshot(REFERENCE_DATE, ASSET_CURRENCY),
			buildSnapshot(PREVIOUS_REFERENCE_DATE, ASSET_CURRENCY)
		);
		List<LocalDate> stubbedDates = List.of(REFERENCE_DATE, PREVIOUS_REFERENCE_DATE);
		SnapshotDTO stubbedFirstDto = Instancio.create(SnapshotDTO.class);
		SnapshotDTO stubbedSecondDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, stubbedDates);
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(snapshotService.findAll(mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0), book)).thenReturn(stubbedFirstDto);
		when(mapper.toDto(stubbedSnapshots.get(1), book)).thenReturn(stubbedSecondDto);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getAllSnapshots(mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedFirstDto, stubbedSecondDto);
	}

	@Test
	void createSnapshot_usesExplicitExchangeRate_whenProvided() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), EXPLICIT_RATE)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), AMOUNT)
			.create();
		Asset mockAsset = buildAsset(ASSET_CURRENCY);
		User mockUser = buildUser(mockUserId, USER_CURRENCY);
		Snapshot mockSaved = buildSnapshot(REFERENCE_DATE, ASSET_CURRENCY);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(REFERENCE_DATE));
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			snapshotService.saveOrUpdate(mockAsset, AMOUNT, mockDto.getReferenceDate(), EXPLICIT_RATE)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved, book)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto, mockUserId);
	}

	@Test
	void createSnapshot_usesOneAsRate_whenCurrenciesMatch() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), AMOUNT)
			.create();
		Asset mockAsset = buildAsset(USER_CURRENCY);
		User mockUser = buildUser(mockUserId, USER_CURRENCY);
		Snapshot mockSaved = buildSnapshot(REFERENCE_DATE, USER_CURRENCY);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(USER_CURRENCY, USER_CURRENCY, List.of(REFERENCE_DATE));
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			snapshotService.saveOrUpdate(mockAsset, AMOUNT, mockDto.getReferenceDate(), BigDecimal.ONE)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved, book)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void createSnapshot_resolvesTheRateFromTheProvider_whenCurrenciesDiffer() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), AMOUNT)
			.create();
		Asset mockAsset = buildAsset(ASSET_CURRENCY);
		User mockUser = buildUser(mockUserId, USER_CURRENCY);
		Snapshot mockSaved = buildSnapshot(REFERENCE_DATE, ASSET_CURRENCY);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(REFERENCE_DATE));
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			exchangeRateProvider.getRate(
				ASSET_CURRENCY.getValue(),
				USER_CURRENCY.getValue(),
				mockDto.getReferenceDate()
			)
		).thenReturn(RATE_TO_USER_CURRENCY);
		when(
			snapshotService.saveOrUpdate(
				mockAsset,
				AMOUNT,
				mockDto.getReferenceDate(),
				RATE_TO_USER_CURRENCY
			)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved, book)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void deleteBulk_delegatesToService() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<UUID> mockIds = List.of(UUID.randomUUID(), UUID.randomUUID());

		// WHEN
		testSubject.deleteBulk(mockAssetId, mockIds, mockUserId);

		// THEN
		verify(snapshotService).deleteBulk(mockIds, mockAssetId, mockUserId);
	}

	private RateBook stubBook(Currency source, Currency target, List<LocalDate> dates) {
		if (source != target) {
			when(exchangeRateProvider.getRates(source.getValue(), target.getValue(), dates)).thenReturn(
				Map.of(dates.get(0), RATE_TO_USER_CURRENCY)
			);
		}
		RateBook book = new CurrencyConversionService(exchangeRateProvider).book(
			target,
			List.of(source),
			dates
		);
		when(conversionService.book(target, List.of(source), dates)).thenReturn(book);
		return book;
	}

	private void stubUserCurrency(UUID userId, Currency currency) {
		when(userRepository.findById(userId)).thenReturn(Optional.of(buildUser(userId, currency)));
	}

	private User buildUser(UUID userId, Currency currency) {
		return Instancio.of(User.class)
			.set(Select.field(User::getId), userId)
			.set(Select.field(User::getCurrency), currency)
			.create();
	}

	private Asset buildAsset(Currency currency) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), currency)
			.create();
	}

	private Snapshot buildSnapshot(LocalDate referenceDate, Currency currency) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), buildAsset(currency))
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), AMOUNT)
			.create();
	}
}
