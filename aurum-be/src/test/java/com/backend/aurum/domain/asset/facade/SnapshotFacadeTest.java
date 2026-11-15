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
import com.backend.aurum.domain.asset.repository.AssetRepository;
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
import java.util.stream.Collectors;
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
	private static final Currency OTHER_ROW_CURRENCY = Currency.GBP;
	private static final Currency CHANGED_ASSET_CURRENCY = Currency.CHF;
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

	@Mock
	private AssetRepository assetRepository;

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
	void getSnapshots_isIdentity_whenTheRowUsesTheUserCurrency() {
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
	void getSnapshots_booksEveryCurrencyTheRowsWereWrittenIn_whenTheAssetCurrencyChangedSince() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = buildAsset(CHANGED_ASSET_CURRENCY);
		List<Snapshot> stubbedSnapshots = List.of(
			buildSnapshot(REFERENCE_DATE, mockAsset, ASSET_CURRENCY),
			buildSnapshot(PREVIOUS_REFERENCE_DATE, mockAsset, OTHER_ROW_CURRENCY)
		);
		List<LocalDate> stubbedDates = List.of(REFERENCE_DATE, PREVIOUS_REFERENCE_DATE);
		List<Currency> stubbedSources = List.of(ASSET_CURRENCY, OTHER_ROW_CURRENCY);
		SnapshotDTO stubbedFirstDto = Instancio.create(SnapshotDTO.class);
		SnapshotDTO stubbedSecondDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(stubbedSources, USER_CURRENCY, stubbedDates);
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0), book)).thenReturn(stubbedFirstDto);
		when(mapper.toDto(stubbedSnapshots.get(1), book)).thenReturn(stubbedSecondDto);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedFirstDto, stubbedSecondDto);
		assertThat(stubbedSnapshots).allSatisfy(row ->
			assertThat(row.getAsset().getOriginalCurrency()).isEqualTo(CHANGED_ASSET_CURRENCY)
		);
		assertThat(
			book.convert(AMOUNT, OTHER_ROW_CURRENCY, PREVIOUS_REFERENCE_DATE)
		).isEqualByComparingTo(AMOUNT.multiply(RATE_TO_USER_CURRENCY));
	}

	@Test
	void getSnapshots_skipsRowsThatResolveNoCurrency_whenTheBookIsBuilt() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = List.of(buildSnapshotWithoutCurrency(REFERENCE_DATE));
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		RateBook book = stubBook(List.of(), USER_CURRENCY, List.of(REFERENCE_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0), book)).thenReturn(stubbedDto);

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto);
		verify(conversionService).book(USER_CURRENCY, List.of(), List.of(REFERENCE_DATE));
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
	void createSnapshot_buildsTheBookFromTheRowCurrency_whenTheAssetChangedCurrencySince() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), AMOUNT)
			.create();
		Asset mockAsset = buildAsset(CHANGED_ASSET_CURRENCY);
		User mockUser = buildUser(mockUserId, USER_CURRENCY);
		Snapshot mockSaved = buildSnapshot(mockDto.getReferenceDate(), mockAsset, ASSET_CURRENCY);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(mockDto.getReferenceDate()));
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			exchangeRateProvider.getRate(
				CHANGED_ASSET_CURRENCY.getValue(),
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
		testSubject = buildFacadeWithRealMapper();

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto.getCurrency()).isEqualTo(ASSET_CURRENCY);
		assertThat(expectedDto.getAssetId()).isEqualTo(mockAsset.getId());
		assertThat(expectedDto.getExchangeRateToBase()).isEqualByComparingTo(RATE_TO_USER_CURRENCY);
		assertThat(expectedDto.getAmountBaseCurrency()).isEqualByComparingTo(
			AMOUNT.multiply(RATE_TO_USER_CURRENCY)
		);
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
		return stubBook(List.of(source), target, dates);
	}

	private RateBook stubBook(List<Currency> sources, Currency target, List<LocalDate> dates) {
		Map<LocalDate, BigDecimal> stubbedRates = dates
			.stream()
			.collect(Collectors.toMap(date -> date, date -> RATE_TO_USER_CURRENCY));
		sources
			.stream()
			.filter(source -> source != target)
			.forEach(source ->
				when(exchangeRateProvider.getRates(source.getValue(), target.getValue(), dates)).thenReturn(
					stubbedRates
				)
			);
		RateBook book = new CurrencyConversionService(exchangeRateProvider).book(
			target,
			sources,
			dates
		);
		when(conversionService.book(target, sources, dates)).thenReturn(book);
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

	private Snapshot buildSnapshot(LocalDate referenceDate, Currency rowCurrency) {
		return buildSnapshot(referenceDate, buildAsset(rowCurrency), rowCurrency);
	}

	private Snapshot buildSnapshot(LocalDate referenceDate, Asset asset, Currency rowCurrency) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), AMOUNT)
			.set(Select.field(Snapshot::getCurrency), rowCurrency)
			.create();
	}

	private Snapshot buildSnapshotWithoutCurrency(LocalDate referenceDate) {
		return buildSnapshot(referenceDate, null, null);
	}

	/**
	 * The row's currency drives both the book and the DTO, and that invariant lives across the
	 * facade and the mapper: a mocked mapper cannot show it, so this case wires the real one.
	 */
	private SnapshotFacade buildFacadeWithRealMapper() {
		return new SnapshotFacade(
			snapshotService,
			new SnapshotMapper(assetRepository),
			validationService,
			assetService,
			exchangeRateProvider,
			conversionService,
			userRepository
		);
	}
}
