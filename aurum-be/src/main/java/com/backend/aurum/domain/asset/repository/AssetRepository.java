package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
	List<Asset> findAllByIsActiveTrue();
	List<Asset> findByUserIdOrderByCreatedAtDesc(UUID userId);
	List<Asset> findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(UUID userId);
	List<Asset> findAllByIsActiveTrueAndLiabilityType(LiabilityType liabilityType);

	@Query(
		"SELECT a FROM Asset a LEFT JOIN FETCH a.snapshots WHERE a.isActive = true AND a.liabilityType = :type"
	)
	List<Asset> findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(@Param("type") LiabilityType type);
}
