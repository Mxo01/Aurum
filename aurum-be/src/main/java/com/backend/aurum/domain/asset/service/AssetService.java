package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AssetService {

	private final AssetRepository assetRepository;
	private final SnapshotRepository snapshotRepository;

	public List<Asset> findAll(UUID userId) {
		return assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	public List<Asset> findAllActive(UUID userId) {
		return assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
	}

	public Asset findById(UUID id, UUID userId) {
		Asset asset = assetRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Asset not found"));

		if (!asset.getUser().getId().equals(userId)) {
			throw new RuntimeException("Access denied: Asset does not belong to user");
		}

		return asset;
	}

	@Transactional
	public Asset save(Asset asset, BigDecimal initialValue, LocalDate referenceDate) {
		Asset savedAsset = assetRepository.save(asset);

		if (initialValue != null && referenceDate != null) {
			Snapshot snapshot = new Snapshot();
			snapshot.setAsset(savedAsset);
			snapshot.setAmountOriginalCurrency(initialValue);
			snapshot.setReferenceDate(referenceDate);
			snapshotRepository.save(snapshot);
			savedAsset.getSnapshots().add(snapshot);
		}

		return savedAsset;
	}

	@Transactional
	public Asset update(UUID id, Asset assetDetails, UUID userId) {
		Asset asset = findById(id, userId);
		asset.setName(assetDetails.getName());
		asset.setCategory(assetDetails.getCategory());
		asset.setOriginalCurrency(assetDetails.getOriginalCurrency());
		asset.setIsActive(assetDetails.getIsActive());
		asset.setIsFavorite(assetDetails.getIsFavorite());
		asset.setUser(assetDetails.getUser());
		return assetRepository.save(asset);
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		Asset asset = findById(id, userId);
		assetRepository.delete(asset);
	}
}
