package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
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
