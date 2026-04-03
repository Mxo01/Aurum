package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.infrastructure.exception.AccessDeniedException;
import com.backend.aurum.infrastructure.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

	private final AssetRepository assetRepository;
	private final SnapshotRepository snapshotRepository;

	@Transactional(readOnly = true)
	public List<Asset> findAll(UUID userId) {
		log.debug("AssetService#findAll - Fetching all assets for userId={}", userId);
		List<Asset> assets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
		log.debug("AssetService#findAll - Found {} asset(s) for userId={}", assets.size(), userId);
		return assets;
	}

	@Transactional(readOnly = true)
	public Asset findById(UUID id, UUID userId) {
		log.debug("AssetService#findById - Looking up assetId={} for userId={}", id, userId);
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("AssetService#findById - Asset not found: assetId={}", id);
				return new NotFoundException("Asset not found");
			});

		if (!asset.getUser().getId().equals(userId)) {
			log.warn(
				"AssetService#findById - Access denied: assetId={} does not belong to userId={}",
				id,
				userId
			);
			throw new AccessDeniedException("Access denied: Asset does not belong to user");
		}

		log.debug("AssetService#findById - Asset found: assetId={}, name={}", id, asset.getName());
		return asset;
	}

	@Transactional
	public Asset save(Asset asset, BigDecimal initialValue, LocalDate referenceDate) {
		log.debug("AssetService#save - Persisting new asset: name={}", asset.getName());
		Asset savedAsset = assetRepository.save(Objects.requireNonNull(asset));
		log.info(
			"AssetService#save - Asset created: assetId={}, name={}",
			savedAsset.getId(),
			savedAsset.getName()
		);

		if (initialValue != null && referenceDate != null) {
			log.debug(
				"AssetService#save - Creating initial snapshot for assetId={}, amount={}, date={}",
				savedAsset.getId(),
				initialValue,
				referenceDate
			);
			Snapshot snapshot = new Snapshot();
			snapshot.setAsset(savedAsset);
			snapshot.setAmountOriginalCurrency(initialValue);
			snapshot.setReferenceDate(referenceDate);
			snapshotRepository.save(Objects.requireNonNull(snapshot));
			savedAsset.getSnapshots().add(snapshot);
			log.debug("AssetService#save - Initial snapshot saved for assetId={}", savedAsset.getId());
		}

		return savedAsset;
	}

	@Transactional
	public Asset update(UUID id, Asset assetDetails, UUID userId) {
		log.debug("AssetService#update - Updating assetId={} for userId={}", id, userId);
		Asset asset = findById(id, userId);

		asset.setName(assetDetails.getName());
		if (assetDetails.getCategory() != null) {
			asset.setCategory(assetDetails.getCategory());
		}
		asset.setOriginalCurrency(assetDetails.getOriginalCurrency());
		asset.setUser(assetDetails.getUser());
		asset.setLiabilityType(assetDetails.getLiabilityType());
		asset.setPaymentFrequency(assetDetails.getPaymentFrequency());
		asset.setPaymentAmount(assetDetails.getPaymentAmount());
		asset.setIsFavorite(Boolean.TRUE.equals(assetDetails.getIsFavorite()));

		Asset updated = assetRepository.save(Objects.requireNonNull(asset));
		log.info(
			"AssetService#update - Asset updated: assetId={}, name={}",
			updated.getId(),
			updated.getName()
		);
		return updated;
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		log.debug("AssetService#delete - Deleting assetId={} for userId={}", id, userId);
		Asset asset = findById(id, userId);
		assetRepository.delete(Objects.requireNonNull(asset));
		log.info("AssetService#delete - Asset deleted: assetId={}", id);
	}
}
