package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.Snapshot;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
	List<Snapshot> findByAssetId(UUID assetId);
	List<Snapshot> findByAssetUserId(UUID userId);
	List<Snapshot> findByAssetUserIdOrderByReferenceDateDesc(UUID userId);
	List<Snapshot> findByAssetUserIdAndReferenceDateBetween(
		UUID userId,
		LocalDate start,
		LocalDate end
	);
	Optional<Snapshot> findFirstByAssetIdAndReferenceDateBetween(
		UUID assetId,
		LocalDate start,
		LocalDate end
	);

	List<Snapshot> findTop2ByAssetIdOrderByReferenceDateDesc(UUID assetId);
}
