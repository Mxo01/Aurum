package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.user.model.User;
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
class SnapshotServiceTest {

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private AssetRepository assetRepository;

	@InjectMocks
	private SnapshotService testSubject;

	@Test
	void findAll_returnsSnapshotsFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(3).create();
		when(snapshotRepository.findByAssetUserId(mockUserId)).thenReturn(stubbedSnapshots);

		// WHEN
		List<Snapshot> expectedSnapshots = testSubject.findAll(mockUserId);

		// THEN
		assertThat(expectedSnapshots).isEqualTo(stubbedSnapshots);
	}

	@Test
	void findByAssetId_returnsSnapshots_whenAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));
		when(snapshotRepository.findByAssetId(mockAsset.getId())).thenReturn(stubbedSnapshots);

		// WHEN
		List<Snapshot> expectedSnapshots = testSubject.findByAssetId(mockAsset.getId(), mockUserId);

		// THEN
		assertThat(expectedSnapshots).isEqualTo(stubbedSnapshots);
	}

	@Test
	void findByAssetId_throwsRuntimeException_whenAssetNotFound() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		when(assetRepository.findById(mockAssetId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findByAssetId(mockAssetId, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void findByAssetId_throwsRuntimeException_whenAssetBelongsToDifferentUser() {
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
		assertThatThrownBy(() -> testSubject.findByAssetId(mockAsset.getId(), mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void saveOrUpdate_updatesExistingSnapshot_whenOneExistsForSameMonth() {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		LocalDate mockDate = LocalDate.of(2025, 6, 15);
		LocalDate startOfMonth = LocalDate.of(2025, 6, 1);
		LocalDate endOfMonth = LocalDate.of(2025, 6, 30);
		Snapshot mockExistingSnapshot = Instancio.create(Snapshot.class);
		Snapshot stubbedSaved = Instancio.create(Snapshot.class);
		when(
			snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(
				mockAsset.getId(),
				startOfMonth,
				endOfMonth
			)
		).thenReturn(Optional.of(mockExistingSnapshot));
		when(snapshotRepository.save(mockExistingSnapshot)).thenReturn(stubbedSaved);

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			BigDecimal.TEN,
			mockDate,
			BigDecimal.ONE
		);

		// THEN
		assertThat(expectedSnapshot).isEqualTo(stubbedSaved);
	}

	@Test
	void saveOrUpdate_createsNewSnapshot_whenNoneExistsForMonth() {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		LocalDate mockDate = LocalDate.of(2025, 6, 15);
		LocalDate startOfMonth = LocalDate.of(2025, 6, 1);
		LocalDate endOfMonth = LocalDate.of(2025, 6, 30);
		Snapshot stubbedSaved = Instancio.create(Snapshot.class);
		when(
			snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(
				mockAsset.getId(),
				startOfMonth,
				endOfMonth
			)
		).thenReturn(Optional.empty());
		when(snapshotRepository.save(org.mockito.ArgumentMatchers.notNull())).thenReturn(stubbedSaved);

		// WHEN
		Snapshot expectedSnapshot = testSubject.saveOrUpdate(
			mockAsset,
			BigDecimal.TEN,
			mockDate,
			BigDecimal.ONE
		);

		// THEN
		assertThat(expectedSnapshot).isEqualTo(stubbedSaved);
	}

	@Test
	void deleteBulk_delegatesToRepository_whenAllSnapshotsAreAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UUID mockAssetId = UUID.randomUUID();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), mockAssetId)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN
		testSubject.deleteBulk(mockIds, mockAssetId, mockUserId);

		// THEN
		verify(snapshotRepository).deleteAllInBatch(List.of(mockSnapshot));
	}

	@Test
	void deleteBulk_throwsRuntimeException_whenSnapshotBelongsToDifferentAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UUID mockAssetId = UUID.randomUUID();
		UUID differentAssetId = UUID.randomUUID();
		Asset mockDifferentAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), differentAssetId)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockDifferentAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void deleteBulk_throwsRuntimeException_whenSnapshotBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		UUID mockAssetId = UUID.randomUUID();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getId), mockAssetId)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.create();
		List<UUID> mockIds = List.of(mockSnapshot.getId());
		when(snapshotRepository.findAllById(mockIds)).thenReturn(List.of(mockSnapshot));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteBulk(mockIds, mockAssetId, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}
}
