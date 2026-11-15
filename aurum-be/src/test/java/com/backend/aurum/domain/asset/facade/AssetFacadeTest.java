package com.backend.aurum.domain.asset.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetUpdate;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
class AssetFacadeTest {

	private static final Currency USER_CURRENCY = Currency.USD;
	private static final Currency ASSET_CURRENCY = Currency.EUR;
	private static final LocalDate TODAY = LocalDate.of(2026, 6, 15);
	private static final LocalDate SNAPSHOT_DATE = LocalDate.of(2026, 5, 31);
	private static final BigDecimal RATE_TO_USER_CURRENCY = new BigDecimal("1.5");
	private static final BigDecimal AMOUNT = new BigDecimal("100");
	private static final String STORED_NAME = "Persisted asset";
	private static final String UPDATED_NAME = "Renamed asset";
	private static final PaymentFrequency STORED_PAYMENT_FREQUENCY = PaymentFrequency.MONTHLY;
	private static final BigDecimal STORED_PAYMENT_AMOUNT = new BigDecimal("200.00");

	@Mock
	private AssetService assetService;

	@Mock
	private AssetMapper mapper;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private AssetCategoryRepository categoryRepository;

	@Mock
	private SnapshotMapper snapshotMapper;

	@Mock
	private AssetValidationService validationService;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrencyConversionService conversionService;

	private final ExchangeRateProvider bookRateProvider = mock(ExchangeRateProvider.class);

	@Mock
	private Clock clock;

	@InjectMocks
	private AssetFacade testSubject;

	@Test
	void getAsset_returnsDtoConvertedIntoTheUserCurrency() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = buildAsset(List.of(buildSnapshot(SNAPSHOT_DATE)));
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(SNAPSHOT_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(assetService.findById(mockAssetId, mockUserId)).thenReturn(mockAsset);
		when(mapper.toDto(mockAsset, book)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.getAsset(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		assertThat(book.convert(AMOUNT, ASSET_CURRENCY, SNAPSHOT_DATE)).isEqualByComparingTo(
			AMOUNT.multiply(RATE_TO_USER_CURRENCY)
		);
	}

	@Test
	void getAsset_isIdentity_whenTheAssetUsesTheUserCurrency() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = buildAsset(List.of(buildSnapshot(SNAPSHOT_DATE)), USER_CURRENCY);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(USER_CURRENCY, USER_CURRENCY, List.of(SNAPSHOT_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(assetService.findById(mockAssetId, mockUserId)).thenReturn(mockAsset);
		when(mapper.toDto(mockAsset, book)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.getAsset(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		assertThat(book.convert(AMOUNT, USER_CURRENCY, SNAPSHOT_DATE)).isEqualByComparingTo(AMOUNT);
	}

	@Test
	void getAsset_buildsTheBookWithoutDates_whenTheAssetHasNoSnapshotCollection() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.set(Select.field(Asset::getSnapshots), null)
			.create();
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = bookWithoutRates(ASSET_CURRENCY, USER_CURRENCY, List.of());
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(assetService.findById(mockAssetId, mockUserId)).thenReturn(mockAsset);
		when(mapper.toDto(mockAsset, book)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.getAsset(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void getAsset_throwsNotFoundException_whenUserIsMissing() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = buildAsset(List.of());
		when(assetService.findById(mockAssetId, mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getAsset(mockAssetId, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void createAsset_returnsDtoConvertedIntoTheUserCurrency() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		Asset mockAsset = buildAsset(List.of());
		Asset mockSaved = buildAsset(List.of(buildSnapshot(SNAPSHOT_DATE)));
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(SNAPSHOT_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockAsset);
		when(
			assetService.save(mockAsset, mockDto.getInitialValue(), mockDto.getReferenceDate())
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved, book)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.createAsset(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateAsset_returnsDtoConvertedIntoTheUserCurrency() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		AssetUpdate mockUpdate = buildUpdate();
		Asset mockUpdated = buildAsset(List.of(buildSnapshot(SNAPSHOT_DATE)));
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(SNAPSHOT_DATE));
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(mapper.toUpdate(mockDto, mockUserId)).thenReturn(mockUpdate);
		when(assetService.update(mockId, mockUpdate, mockUserId)).thenReturn(mockUpdated);
		when(mapper.toDto(mockUpdated, book)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.updateAsset(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateAssetLight_returnsDtoConvertedAtToday() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		AssetUpdate mockUpdate = buildUpdate();
		Asset mockUpdated = buildAsset(List.of());
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(TODAY));
		stubToday();
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(mapper.toUpdate(mockDto, mockUserId)).thenReturn(mockUpdate);
		when(assetService.update(mockId, mockUpdate, mockUserId)).thenReturn(mockUpdated);
		when(snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(mockId)).thenReturn(
			stubbedSnapshots
		);
		when(mapper.toDtoLight(mockUpdated, stubbedSnapshots, book, TODAY)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.updateAssetLight(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateAsset_keepsTheWholeLiabilityBlock_whenTheBodyCarriesOnlyAName() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), mockUserId)
			.set(Select.field(User::getCurrency), USER_CURRENCY)
			.create();
		AssetCategory mockStoredCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		Asset mockStoredAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), mockAssetId)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getCategory), mockStoredCategory)
			.set(Select.field(Asset::getName), STORED_NAME)
			.set(Select.field(Asset::getOriginalCurrency), ASSET_CURRENCY)
			.set(Select.field(Asset::getIsFavorite), true)
			.set(Select.field(Asset::getLiabilityType), LiabilityType.AUTOMATIC)
			.set(Select.field(Asset::getPaymentFrequency), STORED_PAYMENT_FREQUENCY)
			.set(Select.field(Asset::getPaymentAmount), STORED_PAYMENT_AMOUNT)
			.set(Select.field(Asset::getSettledAt), null)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		UpdateAssetDTO mockDto = new UpdateAssetDTO();
		mockDto.setName(UPDATED_NAME);
		RateBook book = bookWithoutRates(ASSET_CURRENCY, USER_CURRENCY, List.of());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(assetRepository.findById(mockAssetId)).thenReturn(Optional.of(mockStoredAsset));
		when(snapshotRepository.existsByAssetId(mockAssetId)).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		testSubject = buildFacadeWithRealMapperAndService();

		// WHEN
		testSubject.updateAsset(mockAssetId, mockDto, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getName()).isEqualTo(UPDATED_NAME);
		assertThat(mockStoredAsset.getCategory()).isSameAs(mockStoredCategory);
		assertThat(mockStoredAsset.getOriginalCurrency()).isEqualTo(ASSET_CURRENCY);
		assertThat(mockStoredAsset.getIsFavorite()).isTrue();
		assertThat(mockStoredAsset.getLiabilityType()).isEqualTo(LiabilityType.AUTOMATIC);
		assertThat(mockStoredAsset.getPaymentFrequency()).isEqualTo(STORED_PAYMENT_FREQUENCY);
		assertThat(mockStoredAsset.getPaymentAmount()).isEqualByComparingTo(STORED_PAYMENT_AMOUNT);
	}

	@Test
	void getAllAssets_returnsLightDtosFromASingleBookPerRequest() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockFirstAsset = buildAsset(List.of());
		Asset mockSecondAsset = buildAsset(List.of());
		Snapshot mockFirstSnapshot = buildSnapshot(SNAPSHOT_DATE, mockFirstAsset);
		AssetDTO stubbedFirstDto = Instancio.create(AssetDTO.class);
		AssetDTO stubbedSecondDto = Instancio.create(AssetDTO.class);
		RateBook book = stubBook(ASSET_CURRENCY, USER_CURRENCY, List.of(TODAY));
		stubToday();
		stubUserCurrency(mockUserId, USER_CURRENCY);
		when(assetService.findAll(mockUserId)).thenReturn(List.of(mockFirstAsset, mockSecondAsset));
		when(snapshotRepository.findByAssetUserIdOrderByReferenceDateDesc(mockUserId)).thenReturn(
			List.of(mockFirstSnapshot)
		);
		when(mapper.toDtoLight(mockFirstAsset, List.of(mockFirstSnapshot), book, TODAY)).thenReturn(
			stubbedFirstDto
		);
		when(mapper.toDtoLight(mockSecondAsset, List.of(), book, TODAY)).thenReturn(stubbedSecondDto);

		// WHEN
		List<AssetDTO> expectedDtos = testSubject.getAllAssets(mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedFirstDto, stubbedSecondDto);
		verify(conversionService, times(1)).book(
			USER_CURRENCY,
			List.of(ASSET_CURRENCY),
			List.of(TODAY)
		);
	}

	@Test
	void getAllAssets_returnsEmptyList_whenTheUserHasNoAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(assetService.findAll(mockUserId)).thenReturn(List.of());

		// WHEN
		List<AssetDTO> expectedDtos = testSubject.getAllAssets(mockUserId);

		// THEN
		assertThat(expectedDtos).isEmpty();
	}

	@Test
	void deleteAsset_delegatesToService() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		testSubject.deleteAsset(mockId, mockUserId);

		// THEN
		verify(assetService).delete(mockId, mockUserId);
	}

	private AssetUpdate buildUpdate() {
		return new AssetUpdate(UPDATED_NAME, Optional.empty(), ASSET_CURRENCY, true, Optional.empty());
	}

	/**
	 * The update invariant lives across the mapper and the service, so this one case wires both for
	 * real: a mocked pair cannot show that a rename reaches the repository with the block intact.
	 */
	private AssetFacade buildFacadeWithRealMapperAndService() {
		return new AssetFacade(
			new AssetService(assetRepository, snapshotRepository, conversionService),
			new AssetMapper(categoryRepository, userRepository, snapshotMapper),
			validationService,
			snapshotRepository,
			userRepository,
			conversionService,
			clock
		);
	}

	private RateBook stubBook(Currency source, Currency target, List<LocalDate> dates) {
		if (source != target) {
			when(bookRateProvider.getRates(source.getValue(), target.getValue(), dates)).thenReturn(
				Map.of(dates.get(0), RATE_TO_USER_CURRENCY)
			);
		}
		RateBook book = new CurrencyConversionService(bookRateProvider).book(
			target,
			List.of(source),
			dates
		);
		when(conversionService.book(target, List.of(source), dates)).thenReturn(book);
		return book;
	}

	private RateBook bookWithoutRates(Currency source, Currency target, List<LocalDate> dates) {
		when(bookRateProvider.getRates(source.getValue(), target.getValue(), dates)).thenReturn(
			Map.of()
		);
		RateBook book = new CurrencyConversionService(bookRateProvider).book(
			target,
			List.of(source),
			dates
		);
		when(conversionService.book(target, List.of(source), dates)).thenReturn(book);
		return book;
	}

	private void stubToday() {
		when(clock.getZone()).thenReturn(ZoneOffset.UTC);
		when(clock.instant()).thenReturn(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant());
	}

	private void stubUserCurrency(UUID userId, Currency currency) {
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), userId)
			.set(Select.field(User::getCurrency), currency)
			.create();
		when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
	}

	private Asset buildAsset(List<Snapshot> snapshots) {
		return buildAsset(snapshots, ASSET_CURRENCY);
	}

	private Asset buildAsset(List<Snapshot> snapshots, Currency currency) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), currency)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>(snapshots))
			.create();
	}

	private Snapshot buildSnapshot(LocalDate referenceDate) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.create();
	}

	private Snapshot buildSnapshot(LocalDate referenceDate, Asset asset) {
		return Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), asset)
			.set(Select.field(Snapshot::getReferenceDate), referenceDate)
			.create();
	}
}
