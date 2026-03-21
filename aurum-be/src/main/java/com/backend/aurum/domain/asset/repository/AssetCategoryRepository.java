package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.AssetCategory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, UUID> {
	List<AssetCategory> findByUserId(UUID userId);

	List<AssetCategory> findByUserIdOrUserIsNull(UUID userId);
}
