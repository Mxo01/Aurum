package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.AssetStatusLog;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetStatusLogRepository extends JpaRepository<AssetStatusLog, UUID> {
	List<AssetStatusLog> findByAssetIdIn(Collection<UUID> assetIds);
}
