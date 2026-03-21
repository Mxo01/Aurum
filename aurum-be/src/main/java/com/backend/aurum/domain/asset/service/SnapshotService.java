package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SnapshotService {

	private final SnapshotRepository snapshotRepository;

	public List<Snapshot> findAll(UUID userId) {
		return snapshotRepository.findByAssetUserId(userId);
	}

	public List<Snapshot> findByAssetId(UUID assetId) {
		return snapshotRepository.findByAssetId(assetId);
	}

	public Snapshot findById(UUID id, UUID userId) {
		Snapshot snapshot = snapshotRepository
			.findById(id)
			.orElseThrow(() -> new RuntimeException("Snapshot not found"));

		if (!snapshot.getAsset().getUser().getId().equals(userId)) {
			throw new RuntimeException("Access denied: Snapshot does not belong to user");
		}
		return snapshot;
	}

	public List<Snapshot> findByDateRange(UUID userId, LocalDate start, LocalDate end) {
		return snapshotRepository
			.findByAssetUserId(userId)
			.stream()
			.filter(s -> !s.getReferenceDate().isBefore(start) && !s.getReferenceDate().isAfter(end))
			.toList();
	}

	public Optional<Snapshot> findExistingForMonth(UUID assetId, LocalDate referenceDate) {
		LocalDate startOfMonth = referenceDate.withDayOfMonth(1);
		LocalDate endOfMonth = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
		return snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(
			assetId,
			startOfMonth,
			endOfMonth
		);
	}

	@Transactional
	public Snapshot save(Snapshot snapshot) {
		return snapshotRepository.save(snapshot);
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		Snapshot snapshot = findById(id, userId);
		snapshotRepository.delete(snapshot);
	}

	@Transactional
	public void deleteBulk(List<UUID> ids, UUID assetId, UUID userId) {
		List<Snapshot> snapshots = snapshotRepository.findAllById(ids);

		for (Snapshot s : snapshots) {
			if (!s.getAsset().getId().equals(assetId)) {
				throw new RuntimeException(
					"Snapshot " + s.getId() + " does not belong to asset " + assetId
				);
			}

			if (!s.getAsset().getUser().getId().equals(userId)) {
				throw new RuntimeException(
					"Access denied: Snapshot " + s.getId() + " does not belong to user"
				);
			}
		}

		snapshotRepository.deleteAllInBatch(snapshots);
	}
}
