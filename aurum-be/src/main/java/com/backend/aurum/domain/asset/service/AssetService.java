package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;

    public List<Asset> findAll(UUID userId) {
        return assetRepository.findByUserId(userId);
    }

    public List<Asset> findAllActive(UUID userId) {
        return assetRepository.findByUserIdAndIsActiveTrue(userId);
    }

    public Asset findById(UUID id) {
        return assetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Asset not found"));
    }

    @Transactional
    public Asset save(Asset asset) {
        return assetRepository.save(asset);
    }

    @Transactional
    public Asset update(UUID id, Asset assetDetails) {
        Asset asset = findById(id);
        asset.setName(assetDetails.getName());
        asset.setCategory(assetDetails.getCategory());
        asset.setOriginalCurrency(assetDetails.getOriginalCurrency());
        asset.setIsActive(assetDetails.getIsActive());
        asset.setIsFavorite(assetDetails.getIsFavorite());
        asset.setUser(assetDetails.getUser());
        return assetRepository.save(asset);
    }

    @Transactional
    public void delete(UUID id) {
        Asset asset = findById(id);
        asset.setIsActive(false);
        assetRepository.save(asset);
    }
}
