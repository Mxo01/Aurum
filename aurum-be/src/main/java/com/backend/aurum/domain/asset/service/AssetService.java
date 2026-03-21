package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AssetService {

	private final AssetRepository assetRepository;
	private final SnapshotRepository snapshotRepository;

	@Transactional(readOnly = true)
	public List<Asset> findAll(UUID userId) {
		return assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public List<Asset> findAllActive(UUID userId) {
		return assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
	}

	@Transactional(readOnly = true)
	public Asset findById(UUID id, UUID userId) {
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> new RuntimeException("Asset not found"));

		if (!asset.getUser().getId().equals(userId)) {
			throw new RuntimeException("Access denied: Asset does not belong to user");
		}

		return asset;
	}

	@Transactional
	public Asset save(Asset asset, BigDecimal initialValue, LocalDate referenceDate) {
		Asset savedAsset = assetRepository.save(Objects.requireNonNull(asset));

		if (initialValue != null && referenceDate != null) {
			Snapshot snapshot = new Snapshot();
			snapshot.setAsset(savedAsset);
			snapshot.setAmountOriginalCurrency(initialValue);
			snapshot.setReferenceDate(referenceDate);
			snapshotRepository.save(Objects.requireNonNull(snapshot));
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
		asset.setUser(assetDetails.getUser());
		asset.setLiabilityType(assetDetails.getLiabilityType());
		asset.setPaymentFrequency(assetDetails.getPaymentFrequency());
		asset.setPaymentAmount(assetDetails.getPaymentAmount());

		boolean willBeActive = Boolean.TRUE.equals(assetDetails.getIsActive());
		if (!willBeActive) {
			asset.setIsFavorite(false);
		} else if (Boolean.TRUE.equals(assetDetails.getIsFavorite())) {
			asset.setIsFavorite(true);
		} else {
			asset.setIsFavorite(false);
		}

		return assetRepository.save(Objects.requireNonNull(asset));
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		Asset asset = findById(id, userId);
		assetRepository.delete(Objects.requireNonNull(asset));
	}
}
