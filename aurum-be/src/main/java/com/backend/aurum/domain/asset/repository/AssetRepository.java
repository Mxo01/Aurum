package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
	@EntityGraph(attributePaths = "category")
	List<Asset> findByUserIdOrderByCreatedAtDesc(UUID userId);

	@Query("SELECT a FROM Asset a LEFT JOIN FETCH a.snapshots WHERE a.liabilityType = :type")
	List<Asset> findAllByLiabilityTypeWithSnapshots(@Param("type") LiabilityType type);
}
