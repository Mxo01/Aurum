package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
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

	private static final PaymentFrequency PERSISTED_PAYMENT_FREQUENCY = PaymentFrequency.MONTHLY;
	private static final BigDecimal PERSISTED_PAYMENT_AMOUNT = new BigDecimal("200.00");
	private static final BigDecimal DIFFERENT_PAYMENT_AMOUNT = new BigDecimal("250.00");
	private static final BigDecimal PERSISTED_PAYMENT_AMOUNT_AT_OTHER_SCALE = new BigDecimal("200");

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
		assertThatThrownBy(() -> testSubject.findById(mockAsset.getId(), mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@ParameterizedTest(name = "no initial snapshot when {0}")
	@MethodSource("incompleteInitialSnapshotInputs")
	void save_createsNoSnapshot_whenInitialSnapshotDataIsIncomplete(
		String scenario,
		BigDecimal mockInitialValue,
		LocalDate mockReferenceDate
	) {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
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
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getOriginalCurrency), mockAssetCurrency)
			.create();
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
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getOriginalCurrency), mockCurrency)
			.create();
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
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), null)
			.set(Select.field(Asset::getOriginalCurrency), mockAssetCurrency)
			.create();
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
	void update_returnsSavedAsset_withUpdatedFields() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockExistingAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Asset mockDetails = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Asset stubbedUpdated = Instancio.create(Asset.class);
		when(assetRepository.findById(mockExistingAsset.getId())).thenReturn(
			Optional.of(mockExistingAsset)
		);
		when(assetRepository.save(mockExistingAsset)).thenReturn(stubbedUpdated);

		// WHEN
		Asset expectedAsset = testSubject.update(mockExistingAsset.getId(), mockDetails, mockUserId);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedUpdated);
	}

	@Test
	void update_keepsExistingCategory_whenDetailsCarryNoCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockExistingCategory = Instancio.create(AssetCategory.class);
		Asset mockExistingAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getCategory), mockExistingCategory)
			.create();
		Asset mockDetails = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getCategory), null)
			.create();
		when(assetRepository.findById(mockExistingAsset.getId())).thenReturn(
			Optional.of(mockExistingAsset)
		);
		when(assetRepository.save(mockExistingAsset)).thenReturn(mockExistingAsset);

		// WHEN
		Asset expectedAsset = testSubject.update(mockExistingAsset.getId(), mockDetails, mockUserId);

		// THEN
		assertThat(expectedAsset.getCategory()).isEqualTo(mockExistingCategory);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("paymentScheduleUpdateInputs")
	void update_appliesScheduleClearingRule_whenPaymentRuleChanges(
		String scenario,
		LiabilityType mockPersistedLiabilityType,
		PaymentFrequency mockPersistedFrequency,
		BigDecimal mockPersistedAmount,
		LiabilityType mockUpdatedLiabilityType,
		PaymentFrequency mockUpdatedFrequency,
		BigDecimal mockUpdatedAmount,
		boolean expectedScheduleCleared
	) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		LocalDate mockPersistedNextDueDate = LocalDate.of(2026, 1, 30);
		Short mockPersistedAnchorDay = (short) 30;
		Asset mockExistingAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getName), "Persisted name")
			.set(Select.field(Asset::getLiabilityType), mockPersistedLiabilityType)
			.set(Select.field(Asset::getPaymentFrequency), mockPersistedFrequency)
			.set(Select.field(Asset::getPaymentAmount), mockPersistedAmount)
			.set(Select.field(Asset::getNextDueDate), mockPersistedNextDueDate)
			.set(Select.field(Asset::getPaymentAnchorDay), mockPersistedAnchorDay)
			.create();
		Asset mockDetails = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getName), "Renamed asset")
			.set(Select.field(Asset::getLiabilityType), mockUpdatedLiabilityType)
			.set(Select.field(Asset::getPaymentFrequency), mockUpdatedFrequency)
			.set(Select.field(Asset::getPaymentAmount), mockUpdatedAmount)
			.create();
		when(assetRepository.findById(mockExistingAsset.getId())).thenReturn(
			Optional.of(mockExistingAsset)
		);
		when(assetRepository.save(mockExistingAsset)).thenReturn(mockExistingAsset);

		// WHEN
		Asset expectedAsset = testSubject.update(mockExistingAsset.getId(), mockDetails, mockUserId);

		// THEN
		assertThat(expectedAsset.getName()).isEqualTo("Renamed asset");
		if (expectedScheduleCleared) {
			assertThat(expectedAsset.getNextDueDate()).isNull();
			assertThat(expectedAsset.getPaymentAnchorDay()).isNull();
		} else {
			assertThat(expectedAsset.getNextDueDate()).isEqualTo(mockPersistedNextDueDate);
			assertThat(expectedAsset.getPaymentAnchorDay()).isEqualTo(mockPersistedAnchorDay);
		}
	}

	static Stream<Arguments> paymentScheduleUpdateInputs() {
		return Stream.of(
			Arguments.of(
				"payment frequency change clears the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.AUTOMATIC,
				PaymentFrequency.WEEKLY,
				PERSISTED_PAYMENT_AMOUNT,
				true
			),
			Arguments.of(
				"payment amount change clears the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				DIFFERENT_PAYMENT_AMOUNT,
				true
			),
			Arguments.of(
				"liability type change clears the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.MANUAL,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				true
			),
			Arguments.of(
				"name only update keeps the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				false
			),
			Arguments.of(
				"same payment amount at a different scale keeps the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT_AT_OTHER_SCALE,
				false
			),
			Arguments.of(
				"first payment amount set on a missing one clears the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				null,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				true
			),
			Arguments.of(
				"payment amount removed by the update clears the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				PERSISTED_PAYMENT_AMOUNT,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				null,
				true
			),
			Arguments.of(
				"missing payment amount on both sides keeps the persisted schedule",
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				null,
				LiabilityType.AUTOMATIC,
				PERSISTED_PAYMENT_FREQUENCY,
				null,
				false
			)
		);
	}

	@Test
	void update_keepsSettledAt_whenThePaymentAmountChanges() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		LocalDateTime mockSettledAt = LocalDateTime.of(2026, 3, 1, 9, 30);
		Asset mockExistingAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getLiabilityType), LiabilityType.AUTOMATIC)
			.set(Select.field(Asset::getPaymentFrequency), PERSISTED_PAYMENT_FREQUENCY)
			.set(Select.field(Asset::getPaymentAmount), PERSISTED_PAYMENT_AMOUNT)
			.set(Select.field(Asset::getNextDueDate), LocalDate.of(2026, 2, 28))
			.set(Select.field(Asset::getPaymentAnchorDay), (short) 28)
			.set(Select.field(Asset::getSettledAt), mockSettledAt)
			.create();
		Asset mockDetails = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getLiabilityType), LiabilityType.AUTOMATIC)
			.set(Select.field(Asset::getPaymentFrequency), PERSISTED_PAYMENT_FREQUENCY)
			.set(Select.field(Asset::getPaymentAmount), DIFFERENT_PAYMENT_AMOUNT)
			.set(Select.field(Asset::getSettledAt), null)
			.create();
		when(assetRepository.findById(mockExistingAsset.getId())).thenReturn(
			Optional.of(mockExistingAsset)
		);
		when(assetRepository.save(mockExistingAsset)).thenReturn(mockExistingAsset);

		// WHEN
		Asset expectedAsset = testSubject.update(mockExistingAsset.getId(), mockDetails, mockUserId);

		// THEN
		assertThat(expectedAsset.getSettledAt()).isEqualTo(mockSettledAt);
		assertThat(expectedAsset.getNextDueDate()).isNull();
		assertThat(expectedAsset.getPaymentAnchorDay()).isNull();
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
}
