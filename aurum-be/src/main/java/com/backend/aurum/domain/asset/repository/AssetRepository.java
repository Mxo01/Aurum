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

	/**
	 * Ids only: the scheduler pays each asset in its own transaction, and a settled liability is not
	 * selected again.
	 */
	@Query(
		"SELECT a.id FROM Asset a WHERE a.liabilityType = :type AND a.settledAt IS NULL ORDER BY a.id"
	)
	List<UUID> findIdsByLiabilityTypeAndSettledAtIsNull(@Param("type") LiabilityType type);

	@Query("SELECT COUNT(a) FROM Asset a WHERE a.category.id = :categoryId")
	long countByCategoryId(@Param("categoryId") UUID categoryId);
}
