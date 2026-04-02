package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exception.AccessDeniedException;
import com.backend.aurum.infrastructure.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
class AssetServiceTest {

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private AssetStatusLogRepository statusLogRepository;

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
	void findById_throwsRuntimeException_whenAssetNotFound() {
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

	@Test
	void save_returnsSavedAsset_withoutSnapshot_whenInitialValueIsNull() {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		Asset stubbedSaved = Instancio.create(Asset.class);
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);

		// WHEN
		Asset expectedAsset = testSubject.save(mockAsset, null, null);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedSaved);
	}

	@Test
	void save_createsInitialSnapshot_whenInitialValueAndDateProvided() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getSnapshots), new java.util.ArrayList<>())
			.create();
		Asset stubbedSaved = new Asset();
		when(assetRepository.save(mockAsset)).thenReturn(stubbedSaved);

		// WHEN
		testSubject.save(mockAsset, BigDecimal.TEN, LocalDate.now());

		// THEN
		assertThat(stubbedSaved.getSnapshots()).hasSize(1);
		Snapshot createdSnapshot = stubbedSaved.getSnapshots().get(0);
		assertThat(createdSnapshot.getAmountOriginalCurrency()).isEqualTo(BigDecimal.TEN);
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
	void update_savesStatusLog_whenActiveStatusChanges() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockExistingAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getIsActive), true)
			.create();
		Asset mockDetails = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getIsActive), false)
			.create();
		when(assetRepository.findById(mockExistingAsset.getId())).thenReturn(
			Optional.of(mockExistingAsset)
		);
		when(assetRepository.save(mockExistingAsset)).thenReturn(mockExistingAsset);

		// WHEN
		testSubject.update(mockExistingAsset.getId(), mockDetails, mockUserId);

		// THEN
		verify(statusLogRepository).save(org.mockito.ArgumentMatchers.notNull());
	}

	@Test
	void patchStatus_returnsUpdatedAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Asset stubbedUpdated = Instancio.create(Asset.class);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));
		when(assetRepository.save(mockAsset)).thenReturn(stubbedUpdated);

		// WHEN
		Asset expectedAsset = testSubject.patchStatus(
			mockAsset.getId(),
			false,
			LocalDate.now(),
			mockUserId
		);

		// THEN
		assertThat(expectedAsset).isEqualTo(stubbedUpdated);
	}

	@Test
	void patchStatus_setsFavoriteToFalse_whenDeactivating() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.set(Select.field(Asset::getIsFavorite), true)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));
		when(assetRepository.save(mockAsset)).thenReturn(mockAsset);

		// WHEN
		testSubject.patchStatus(mockAsset.getId(), false, LocalDate.now(), mockUserId);

		// THEN
		assertThat(mockAsset.getIsFavorite()).isFalse();
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
