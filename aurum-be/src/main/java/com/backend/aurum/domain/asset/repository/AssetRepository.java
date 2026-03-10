package com.backend.aurum.domain.asset.repository;

import com.backend.aurum.domain.asset.model.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {

    List<Asset> findAllByIsActiveTrue();
}
