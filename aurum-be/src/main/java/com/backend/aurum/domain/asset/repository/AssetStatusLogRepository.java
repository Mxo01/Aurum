package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.AssetStatusLog;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetStatusLogRepository extends JpaRepository<AssetStatusLog, UUID> {
	List<AssetStatusLog> findByAssetIdIn(Collection<UUID> assetIds);

	Optional<AssetStatusLog> findTopByAssetIdOrderByChangedAtDesc(UUID assetId);

	@Modifying
	@Query("DELETE FROM AssetStatusLog l WHERE l.changedAt < :cutoff")
	int deleteByChangedAtBefore(@Param("cutoff") LocalDate cutoff);
}
