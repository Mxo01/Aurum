package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SnapshotRepository extends JpaRepository<Snapshot, UUID> {
	List<Snapshot> findByAssetId(UUID assetId);
	List<Snapshot> findByAssetUserId(UUID userId);
	List<Snapshot> findByAssetUserIdOrderByReferenceDateDesc(UUID userId);
	Optional<Snapshot> findFirstByAssetIdAndSourceAndReferenceDateBetween(
		UUID assetId,
		SnapshotSource source,
		LocalDate start,
		LocalDate end
	);

	// A same-date user/scheduler pair must resolve the same way on every run: the payment chain
	// reads a stable outstanding balance.
	Optional<Snapshot> findFirstByAssetIdOrderByReferenceDateDescIdAsc(UUID assetId);

	List<Snapshot> findTop2ByAssetIdOrderByReferenceDateDescIdAsc(UUID assetId);

	boolean existsByAssetId(UUID assetId);

	List<Snapshot> findByAssetUserIdAndReferenceDateGreaterThanEqual(UUID userId, LocalDate since);

	/**
	 * Newest snapshot of every asset of the user, with no lower bound. Same-date rows are all
	 * returned: the caller breaks the tie by lowest id, as every other read does.
	 */
	@Query(
		"SELECT s FROM Snapshot s WHERE s.asset.user.id = :userId AND s.referenceDate = " +
			"(SELECT MAX(latest.referenceDate) FROM Snapshot latest WHERE latest.asset = s.asset)"
	)
	List<Snapshot> findLatestPerAssetByUserId(@Param("userId") UUID userId);

	@Query("SELECT MIN(s.referenceDate) FROM Snapshot s WHERE s.asset.user.id = :userId")
	Optional<LocalDate> findOldestReferenceDateByAssetUserId(@Param("userId") UUID userId);
}
