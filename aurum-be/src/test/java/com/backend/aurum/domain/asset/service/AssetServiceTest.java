package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.asset.model.AssetUpdate;
import com.backend.aurum.domain.asset.model.LiabilityConfiguration;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private CurrencyConversionService conversionService;

	@InjectMocks
	private AssetService testSubject;

	private static final String STORED_NAME = "Persisted asset";
	private static final String UPDATED_NAME = "Renamed asset";
	private static final Currency STORED_CURRENCY = Currency.EUR;
	private static final Currency UPDATED_CURRENCY = Currency.USD;
	private static final PaymentFrequency STORED_PAYMENT_FREQUENCY = PaymentFrequency.MONTHLY;
	private static final PaymentFrequency UPDATED_PAYMENT_FREQUENCY = PaymentFrequency.WEEKLY;
	private static final BigDecimal STORED_PAYMENT_AMOUNT = new BigDecimal("200.00");
	private static final BigDecimal STORED_PAYMENT_AMOUNT_AT_OTHER_SCALE = new BigDecimal("200");
	private static final BigDecimal UPDATED_PAYMENT_AMOUNT = new BigDecimal("250.00");
	private static final BigDecimal NO_PAYMENT_AMOUNT = BigDecimal.ZERO;
	private static final LocalDate STORED_NEXT_DUE_DATE = LocalDate.of(2026, 1, 30);
	private static final short STORED_PAYMENT_ANCHOR_DAY = 30;
	private static final AssetType ASSET_CATEGORY_TYPE = AssetType.ASSET;
	private static final AssetType LIABILITY_CATEGORY_TYPE = AssetType.LIABILITY;
	private static final LocalDate REFERENCE_DATE = LocalDate.of(2025, 6, 15);
	private static final String LIABILITY_OUTSIDE_LIABILITY_CATEGORY_MESSAGE =
		"Liability type is only allowed on a liability category";
	private static final String MISSING_LIABILITY_TYPE_MESSAGE =
		"Liability type is required when a payment frequency or amount is set";
	private static final String NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE =
		"Payment amount must be greater than zero for an automatic liability";
	private static final String ASSET_NOT_OWNED_MESSAGE = "This asset does not belong to you";
	private static final String MISSING_SNAPSHOT_MESSAGE =
		"An automatic liability needs a snapshot before it can be updated";

	@Test
	void findAll_returnsAssetsFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Asset> stubbedAssets = Instancio.ofList(Asset.class).size(3).create();
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(stubbedAssets);

		// WHEN
		List<Asset> expectedAssets = testSubject.findAll(mockUserId);

		// THEN
		assertThat(expectedAssets).isEqualTo(stubbedAssets);
	}

	@Test
	void findById_returnsAsset_whenAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Asset expectedAsset = testSubject.findById(mockAsset.getId(), mockUserId);

		// THEN
		assertThat(expectedAsset).isEqualTo(mockAsset);
	}

	@Test
	void findById_throwsNotFoundException_whenAssetNotFound() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findById(mockAssetId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findById(mockAssetId, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void findById_throwsAccessDeniedException_whenAssetBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findById(mockAsset.getId(), mockUserId))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage(ASSET_NOT_OWNED_MESSAGE);
	}

	@ParameterizedTest(name = "no initial snapshot when {0}")
	@MethodSource("incompleteInitialSnapshotInputs")
	void save_createsNoSnapshot_whenInitialSnapshotDataIsIncomplete(
		String scenario,
		BigDecimal mockInitialValue,
		LocalDate mockReferenceDate
	) {
		// GIVEN
		Asset mockAsset = newAsset(noLiability(), ASSET_CATEGORY_TYPE);
		Asset stubbedSaved = Instancio.of(Asset.class)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);

		// WHEN
		Asset expectedAsset = testSubject.save(mockAsset, mockInitialValue, mockReferenceDate);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedSaved);
		assertThat(stubbedSaved.getSnapshots()).isEmpty();
	}

	static Stream<Arguments> incompleteInitialSnapshotInputs() {
		return Stream.of(
			Arguments.of("initial value is null", null, LocalDate.of(2025, 6, 15)),
			Arguments.of("reference date is null", BigDecimal.TEN, null)
		);
	}

	@Test
	void save_createsInitialSnapshot_withRateFromAssetCurrencyToUserCurrencyAtReferenceDate() {
		// GIVEN
		Currency mockAssetCurrency = Currency.EUR;
		Currency mockUserCurrency = Currency.USD;
		LocalDate mockReferenceDate = LocalDate.of(2025, 6, 15);
		BigDecimal mockInitialValue = new BigDecimal("2500.0000");
		BigDecimal stubbedRate = new BigDecimal("1.084200");
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getCurrency), mockUserCurrency)
			.create();
		Asset mockAsset = newAsset(
			automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
			LIABILITY_CATEGORY_TYPE
		);
		mockAsset.setUser(mockUser);
		mockAsset.setOriginalCurrency(mockAssetCurrency);
		Asset stubbedSaved = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getOriginalCurrency), mockAssetCurrency)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		Snapshot mockPersistedSnapshot = Instancio.create(Snapshot.class);
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);
		when(snapshotRepository.save(notNull())).thenReturn(mockPersistedSnapshot);
		when(conversionService.rate(mockAssetCurrency, mockUserCurrency, mockReferenceDate)).thenReturn(
			stubbedRate
		);

		// WHEN
		testSubject.save(mockAsset, mockInitialValue, mockReferenceDate);

		// THEN
		assertThat(stubbedSaved.getSnapshots()).hasSize(1);
		Snapshot expectedSnapshot = stubbedSaved.getSnapshots().get(0);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualTo(stubbedRate);
		assertThat(expectedSnapshot.getAmountOriginalCurrency()).isEqualTo(mockInitialValue);
		assertThat(expectedSnapshot.getReferenceDate()).isEqualTo(mockReferenceDate);
		assertThat(expectedSnapshot.getAsset()).isEqualTo(stubbedSaved);
		assertThat(expectedSnapshot.getCurrency()).isEqualTo(mockAssetCurrency);
		assertThat(expectedSnapshot.getCurrency()).isNotEqualTo(mockUserCurrency);
	}

	@Test
	void save_storesIdentityRate_whenAssetCurrencyMatchesUserCurrency() {
		// GIVEN
		Currency mockCurrency = Currency.EUR;
		LocalDate mockReferenceDate = LocalDate.of(2025, 6, 15);
		BigDecimal stubbedIdentityRate = BigDecimal.ONE;
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getCurrency), mockCurrency)
			.create();
		Asset mockAsset = newAsset(
			automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
			LIABILITY_CATEGORY_TYPE
		);
		mockAsset.setUser(mockUser);
		mockAsset.setOriginalCurrency(mockCurrency);
		Asset stubbedSaved = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getOriginalCurrency), mockCurrency)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);
		when(snapshotRepository.save(notNull())).thenReturn(Instancio.create(Snapshot.class));
		when(conversionService.rate(mockCurrency, mockCurrency, mockReferenceDate)).thenReturn(
			stubbedIdentityRate
		);

		// WHEN
		testSubject.save(mockAsset, BigDecimal.TEN, mockReferenceDate);

		// THEN
		assertThat(stubbedSaved.getSnapshots()).hasSize(1);
		Snapshot expectedSnapshot = stubbedSaved.getSnapshots().get(0);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualTo(stubbedIdentityRate);
	}

	@Test
	void save_fallsBackToAssetCurrency_whenAssetHasNoUser() {
		// GIVEN
		Currency mockAssetCurrency = Currency.GBP;
		LocalDate mockReferenceDate = LocalDate.of(2025, 6, 15);
		BigDecimal stubbedRate = new BigDecimal("0.860000");
		Asset mockAsset = newAsset(
			automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
			LIABILITY_CATEGORY_TYPE
		);
		mockAsset.setUser(null);
		mockAsset.setOriginalCurrency(mockAssetCurrency);
		Asset stubbedSaved = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), null)
			.set(Select.field(Asset::getOriginalCurrency), mockAssetCurrency)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);
		when(snapshotRepository.save(notNull())).thenReturn(Instancio.create(Snapshot.class));
		when(
			conversionService.rate(mockAssetCurrency, mockAssetCurrency, mockReferenceDate)
		).thenReturn(stubbedRate);

		// WHEN
		testSubject.save(mockAsset, BigDecimal.TEN, mockReferenceDate);

		// THEN
		assertThat(stubbedSaved.getSnapshots()).hasSize(1);
		Snapshot expectedSnapshot = stubbedSaved.getSnapshots().get(0);
		assertThat(expectedSnapshot.getExchangeRateToBase()).isEqualTo(stubbedRate);
	}

	@Test
	void save_throwsValidationException_whenALiabilityTypeSitsInAnAssetCategory() {
		// GIVEN
		Asset mockAsset = newAsset(
			automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
			ASSET_CATEGORY_TYPE
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.save(mockAsset, BigDecimal.TEN, REFERENCE_DATE))
			.isInstanceOf(ValidationException.class)
			.hasMessage(LIABILITY_OUTSIDE_LIABILITY_CATEGORY_MESSAGE);
		verifyNoInteractions(assetRepository, snapshotRepository);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedNewAssetInputs")
	void save_storesTheAsset_whenTheLiabilityBlockMatchesTheCategory(
		String scenario,
		LiabilityConfiguration mockLiability,
		AssetType mockCategoryType
	) {
		// GIVEN
		Asset mockAsset = newAsset(mockLiability, mockCategoryType);
		Asset stubbedSaved = Instancio.of(Asset.class)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>())
			.create();
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);

		// WHEN
		Asset expectedAsset = testSubject.save(mockAsset, null, null);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedSaved);
	}

	static Stream<Arguments> acceptedNewAssetInputs() {
		return Stream.of(
			Arguments.of(
				"a manual liability in a liability category",
				manualLiability(),
				LIABILITY_CATEGORY_TYPE
			),
			Arguments.of(
				"an automatic liability in a liability category",
				new LiabilityConfiguration(
					LiabilityType.AUTOMATIC,
					STORED_PAYMENT_FREQUENCY,
					STORED_PAYMENT_AMOUNT
				),
				LIABILITY_CATEGORY_TYPE
			),
			Arguments.of("no liability block in an asset category", noLiability(), ASSET_CATEGORY_TYPE),
			Arguments.of(
				"no liability block in a liability category",
				noLiability(),
				LIABILITY_CATEGORY_TYPE
			),
			Arguments.of(
				"a liability block without a category",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				null
			)
		);
	}

	@Test
	void update_returnsTheSavedAsset_andAppliesTheName() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		Asset stubbedUpdated = Instancio.create(Asset.class);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(stubbedUpdated);

		// WHEN
		Asset expectedAsset = testSubject.update(
			mockStoredAsset.getId(),
			renameOnlyUpdate(),
			mockUserId
		);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedUpdated);
		assertThat(mockStoredAsset.getName()).isEqualTo(UPDATED_NAME);
	}

	@Test
	void update_keepsTheWholeLiabilityBlock_whenTheBodyCarriesOnlyAName() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		AssetCategory mockStoredCategory = mockStoredAsset.getCategory();
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), renameOnlyUpdate(), mockUserId);

		// THEN
		assertThat(mockStoredAsset.getName()).isEqualTo(UPDATED_NAME);
		assertThat(mockStoredAsset.getCategory()).isSameAs(mockStoredCategory);
		assertThat(mockStoredAsset.getOriginalCurrency()).isEqualTo(STORED_CURRENCY);
		assertThat(mockStoredAsset.getIsFavorite()).isTrue();
		assertThat(mockStoredAsset.getLiabilityType()).isEqualTo(LiabilityType.AUTOMATIC);
		assertThat(mockStoredAsset.getPaymentFrequency()).isEqualTo(STORED_PAYMENT_FREQUENCY);
		assertThat(mockStoredAsset.getPaymentAmount()).isEqualByComparingTo(STORED_PAYMENT_AMOUNT);
		assertThat(mockStoredAsset.getNextDueDate()).isEqualTo(STORED_NEXT_DUE_DATE);
		assertThat(mockStoredAsset.getPaymentAnchorDay()).isEqualTo(STORED_PAYMENT_ANCHOR_DAY);
	}

	@Test
	void update_appliesTheProvidedCurrencyAndFavorite_keepingTheStoredName() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			UPDATED_CURRENCY,
			false,
			Optional.empty()
		);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getName()).isEqualTo(STORED_NAME);
		assertThat(mockStoredAsset.getOriginalCurrency()).isEqualTo(UPDATED_CURRENCY);
		assertThat(mockStoredAsset.getIsFavorite()).isFalse();
	}

	@Test
	void update_appliesTheProvidedCategory_whenTheBodyCarriesOne() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		AssetCategory mockUpdatedCategory = categoryOf(LIABILITY_CATEGORY_TYPE);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.of(mockUpdatedCategory),
			null,
			null,
			Optional.empty()
		);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getCategory()).isSameAs(mockUpdatedCategory);
		assertThat(mockStoredAsset.getName()).isEqualTo(STORED_NAME);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("paymentScheduleUpdateInputs")
	void update_appliesTheScheduleClearingRule_whenThePaymentRuleChanges(
		String scenario,
		LiabilityConfiguration mockStoredLiability,
		Optional<LiabilityConfiguration> mockUpdatedLiability,
		boolean expectedScheduleCleared
	) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId), mockStoredLiability);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		lenient().when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			UPDATED_NAME,
			Optional.empty(),
			null,
			null,
			mockUpdatedLiability
		);

		// WHEN
		Asset expectedAsset = testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(expectedAsset.getName()).isEqualTo(UPDATED_NAME);
		if (expectedScheduleCleared) {
			assertThat(expectedAsset.getNextDueDate()).isNull();
			assertThat(expectedAsset.getPaymentAnchorDay()).isNull();
		} else {
			assertThat(expectedAsset.getNextDueDate()).isEqualTo(STORED_NEXT_DUE_DATE);
			assertThat(expectedAsset.getPaymentAnchorDay()).isEqualTo(STORED_PAYMENT_ANCHOR_DAY);
		}
	}

	static Stream<Arguments> paymentScheduleUpdateInputs() {
		return Stream.of(
			Arguments.of(
				"a frequency change clears the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(LiabilityType.AUTOMATIC, UPDATED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				true
			),
			Arguments.of(
				"an amount change clears the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, UPDATED_PAYMENT_AMOUNT),
				true
			),
			Arguments.of(
				"a type change clears the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(LiabilityType.MANUAL, STORED_PAYMENT_FREQUENCY, NO_PAYMENT_AMOUNT),
				true
			),
			Arguments.of(
				"a rename keeps the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				Optional.<LiabilityConfiguration>empty(),
				false
			),
			Arguments.of(
				"a repeated block keeps the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				false
			),
			Arguments.of(
				"the same amount at another scale keeps the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(
					LiabilityType.AUTOMATIC,
					STORED_PAYMENT_FREQUENCY,
					STORED_PAYMENT_AMOUNT_AT_OTHER_SCALE
				),
				false
			),
			Arguments.of(
				"a first payment rule clears the schedule",
				noLiability(),
				liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				true
			),
			Arguments.of(
				"clearing the block clears the schedule",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				liabilityOf(null, null, null),
				true
			)
		);
	}

	@Test
	void update_replacesTheLiabilityBlock_whenTheTypeIsProvided() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId), noLiability());
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			null,
			null,
			liabilityOf(LiabilityType.AUTOMATIC, UPDATED_PAYMENT_FREQUENCY, UPDATED_PAYMENT_AMOUNT)
		);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getLiabilityType()).isEqualTo(LiabilityType.AUTOMATIC);
		assertThat(mockStoredAsset.getPaymentFrequency()).isEqualTo(UPDATED_PAYMENT_FREQUENCY);
		assertThat(mockStoredAsset.getPaymentAmount()).isEqualByComparingTo(UPDATED_PAYMENT_AMOUNT);
		assertThat(mockStoredAsset.getNextDueDate()).isNull();
	}

	@Test
	void update_clearsTheWholeLiabilityBlock_whenTheTypeIsSentAsNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			null,
			null,
			liabilityOf(null, null, null)
		);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getLiabilityType()).isNull();
		assertThat(mockStoredAsset.getPaymentFrequency()).isNull();
		assertThat(mockStoredAsset.getPaymentAmount()).isNull();
		assertThat(mockStoredAsset.getNextDueDate()).isNull();
		assertThat(mockStoredAsset.getPaymentAnchorDay()).isNull();
	}

	@Test
	void update_throwsValidationException_whenAPaymentRuleArrivesWithoutTheLiabilityType() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			null,
			null,
			liabilityOf(null, UPDATED_PAYMENT_FREQUENCY, null)
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId))
			.isInstanceOf(ValidationException.class)
			.hasMessage(MISSING_LIABILITY_TYPE_MESSAGE);
		assertThat(mockStoredAsset.getLiabilityType()).isEqualTo(LiabilityType.AUTOMATIC);
		assertThat(mockStoredAsset.getPaymentFrequency()).isEqualTo(STORED_PAYMENT_FREQUENCY);
	}

	@Test
	void update_throwsValidationException_whenTheMergedStoredStateIsInvalid() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(
			userWithId(mockUserId),
			automaticLiability(STORED_PAYMENT_FREQUENCY, null)
		);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.update(mockStoredAsset.getId(), renameOnlyUpdate(), mockUserId)
		)
			.isInstanceOf(ValidationException.class)
			.hasMessage(NON_POSITIVE_AUTOMATIC_AMOUNT_MESSAGE);
		assertThat(mockStoredAsset.getName()).isEqualTo(STORED_NAME);
	}

	@Test
	void update_throwsValidationException_whenAnAutomaticLiabilityHasNoSnapshot() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId), noLiability());
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(false);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			null,
			null,
			liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT)
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId))
			.isInstanceOf(ValidationException.class)
			.hasMessage(MISSING_SNAPSHOT_MESSAGE);
		assertThat(mockStoredAsset.getLiabilityType()).isNull();
	}

	@Test
	void update_doesNotRequireASnapshot_whenTheMergedLiabilityIsNotAutomatic() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(
			userWithId(mockUserId),
			new LiabilityConfiguration(LiabilityType.MANUAL, STORED_PAYMENT_FREQUENCY, NO_PAYMENT_AMOUNT)
		);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), renameOnlyUpdate(), mockUserId);

		// THEN
		assertThat(mockStoredAsset.getName()).isEqualTo(UPDATED_NAME);
		verifyNoInteractions(snapshotRepository);
	}

	@Test
	void update_keepsSettledAt_whenThePaymentAmountChanges() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		LocalDateTime mockSettledAt = LocalDateTime.of(2026, 3, 1, 9, 30);
		Asset mockStoredAsset = buildStoredAsset(userWithId(mockUserId));
		mockStoredAsset.setSettledAt(mockSettledAt);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(snapshotRepository.existsByAssetId(mockStoredAsset.getId())).thenReturn(true);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.empty(),
			null,
			null,
			liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, UPDATED_PAYMENT_AMOUNT)
		);

		// WHEN
		Asset expectedAsset = testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(expectedAsset.getSettledAt()).isEqualTo(mockSettledAt);
		assertThat(expectedAsset.getNextDueDate()).isNull();
		assertThat(expectedAsset.getPaymentAnchorDay()).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("liabilityCategoryMismatchUpdates")
	void update_throwsValidationException_whenTheResultingStateLeavesALiabilityInAnAssetCategory(
		String scenario,
		LiabilityConfiguration mockStoredLiability,
		AssetType mockStoredCategoryType,
		AssetUpdate mockUpdate
	) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(
			userWithId(mockUserId),
			mockStoredLiability,
			mockStoredCategoryType
		);
		AssetCategory mockStoredCategory = mockStoredAsset.getCategory();
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId))
			.isInstanceOf(ValidationException.class)
			.hasMessage(LIABILITY_OUTSIDE_LIABILITY_CATEGORY_MESSAGE);
		verify(assetRepository, never()).save(any(Asset.class));
		assertThat(mockStoredAsset.getName()).isEqualTo(STORED_NAME);
		assertThat(mockStoredAsset.getCategory()).isSameAs(mockStoredCategory);
	}

	static Stream<Arguments> liabilityCategoryMismatchUpdates() {
		return Stream.of(
			Arguments.of(
				"a liability block added to an asset category",
				noLiability(),
				ASSET_CATEGORY_TYPE,
				updateOf(
					null,
					Optional.empty(),
					null,
					null,
					liabilityOf(LiabilityType.AUTOMATIC, STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT)
				)
			),
			Arguments.of(
				"a rename on a row that already carries a liability in an asset category",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				ASSET_CATEGORY_TYPE,
				renameOnlyUpdate()
			),
			Arguments.of(
				"a category switch to an asset category that keeps the automatic liability",
				automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
				LIABILITY_CATEGORY_TYPE,
				updateOf(null, Optional.of(categoryOf(ASSET_CATEGORY_TYPE)), null, null, Optional.empty())
			)
		);
	}

	@Test
	void update_acceptsACategorySwitchToALiabilityCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Asset mockStoredAsset = buildStoredAsset(
			userWithId(mockUserId),
			noLiability(),
			ASSET_CATEGORY_TYPE
		);
		AssetCategory mockLiabilityCategory = categoryOf(LIABILITY_CATEGORY_TYPE);
		when(assetRepository.findById(mockStoredAsset.getId())).thenReturn(
			Optional.of(mockStoredAsset)
		);
		when(assetRepository.save(mockStoredAsset)).thenReturn(mockStoredAsset);
		AssetUpdate mockUpdate = updateOf(
			null,
			Optional.of(mockLiabilityCategory),
			null,
			null,
			liabilityOf(LiabilityType.MANUAL, STORED_PAYMENT_FREQUENCY, NO_PAYMENT_AMOUNT)
		);

		// WHEN
		testSubject.update(mockStoredAsset.getId(), mockUpdate, mockUserId);

		// THEN
		assertThat(mockStoredAsset.getCategory()).isSameAs(mockLiabilityCategory);
		assertThat(mockStoredAsset.getLiabilityType()).isEqualTo(LiabilityType.MANUAL);
		assertThat(mockStoredAsset.getPaymentAmount()).isEqualByComparingTo(NO_PAYMENT_AMOUNT);
	}

	@Test
	void delete_delegatesToRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		testSubject.delete(mockAsset.getId(), mockUserId);

		// THEN
		verify(assetRepository).delete(mockAsset);
	}

	private Asset buildStoredAsset(User user) {
		return buildStoredAsset(
			user,
			automaticLiability(STORED_PAYMENT_FREQUENCY, STORED_PAYMENT_AMOUNT),
			LIABILITY_CATEGORY_TYPE
		);
	}

	private Asset buildStoredAsset(User user, LiabilityConfiguration liability) {
		return buildStoredAsset(user, liability, LIABILITY_CATEGORY_TYPE);
	}

	private Asset buildStoredAsset(
		User user,
		LiabilityConfiguration liability,
		AssetType categoryType
	) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), user)
			.set(Select.field(Asset::getName), STORED_NAME)
			.set(Select.field(Asset::getCategory), categoryOf(categoryType))
			.set(Select.field(Asset::getOriginalCurrency), STORED_CURRENCY)
			.set(Select.field(Asset::getIsFavorite), true)
			.set(Select.field(Asset::getLiabilityType), liability.type())
			.set(Select.field(Asset::getPaymentFrequency), liability.frequency())
			.set(Select.field(Asset::getPaymentAmount), liability.amount())
			.set(Select.field(Asset::getNextDueDate), STORED_NEXT_DUE_DATE)
			.set(Select.field(Asset::getPaymentAnchorDay), STORED_PAYMENT_ANCHOR_DAY)
			.set(Select.field(Asset::getSettledAt), null)
			.create();
	}

	private Asset newAsset(LiabilityConfiguration liability, AssetType categoryType) {
		return Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), categoryType == null ? null : categoryOf(categoryType))
			.set(Select.field(Asset::getLiabilityType), liability.type())
			.set(Select.field(Asset::getPaymentFrequency), liability.frequency())
			.set(Select.field(Asset::getPaymentAmount), liability.amount())
			.create();
	}

	private static AssetCategory categoryOf(AssetType type) {
		return Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getType), type)
			.create();
	}

	private static AssetUpdate renameOnlyUpdate() {
		return updateOf(UPDATED_NAME, Optional.empty(), null, null, Optional.empty());
	}

	private static AssetUpdate updateOf(
		String name,
		Optional<AssetCategory> category,
		Currency originalCurrency,
		Boolean isFavorite,
		Optional<LiabilityConfiguration> liability
	) {
		return new AssetUpdate(name, category, originalCurrency, isFavorite, liability);
	}

	private User userWithId(UUID userId) {
		return Instancio.of(User.class).set(Select.field(User::getId), userId).create();
	}

	private static Optional<LiabilityConfiguration> liabilityOf(
		LiabilityType type,
		PaymentFrequency frequency,
		BigDecimal amount
	) {
		return Optional.of(new LiabilityConfiguration(type, frequency, amount));
	}

	private static LiabilityConfiguration automaticLiability(
		PaymentFrequency frequency,
		BigDecimal amount
	) {
		return new LiabilityConfiguration(LiabilityType.AUTOMATIC, frequency, amount);
	}

	private static LiabilityConfiguration manualLiability() {
		return new LiabilityConfiguration(
			LiabilityType.MANUAL,
			STORED_PAYMENT_FREQUENCY,
			NO_PAYMENT_AMOUNT
		);
	}

	private static LiabilityConfiguration noLiability() {
		return new LiabilityConfiguration(null, null, null);
	}
}
