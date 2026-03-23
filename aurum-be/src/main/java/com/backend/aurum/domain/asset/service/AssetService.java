package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetStatusLog;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
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
	private final AssetStatusLogRepository statusLogRepository;

	@Transactional(readOnly = true)
	public List<Asset> findAll(UUID userId) {
		log.debug("AssetService#findAll - Fetching all assets for userId={}", userId);
		List<Asset> assets = assetRepository.findByUserIdOrderByCreatedAtDesc(userId);
		log.debug("AssetService#findAll - Found {} asset(s) for userId={}", assets.size(), userId);
		return assets;
	}

	@Transactional(readOnly = true)
	public List<Asset> findAllActive(UUID userId) {
		log.debug("AssetService#findAllActive - Fetching active assets for userId={}", userId);
		List<Asset> assets = assetRepository.findByUserIdAndIsActiveTrueOrderByCreatedAtDesc(userId);
		log.debug(
			"AssetService#findAllActive - Found {} active asset(s) for userId={}",
			assets.size(),
			userId
		);
		return assets;
	}

	@Transactional(readOnly = true)
	public Asset findById(UUID id, UUID userId) {
		log.debug("AssetService#findById - Looking up assetId={} for userId={}", id, userId);
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("AssetService#findById - Asset not found: assetId={}", id);
				return new RuntimeException("Asset not found");
			});

		if (!asset.getUser().getId().equals(userId)) {
			log.warn(
				"AssetService#findById - Access denied: assetId={} does not belong to userId={}",
				id,
				userId
			);
			throw new RuntimeException("Access denied: Asset does not belong to user");
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
		boolean wasActive = Boolean.TRUE.equals(asset.getIsActive());
		boolean willBeActive = Boolean.TRUE.equals(assetDetails.getIsActive());

		asset.setName(assetDetails.getName());
		if (assetDetails.getCategory() != null) {
			asset.setCategory(assetDetails.getCategory());
		}
		asset.setOriginalCurrency(assetDetails.getOriginalCurrency());
		asset.setIsActive(assetDetails.getIsActive());
		asset.setUser(assetDetails.getUser());
		asset.setLiabilityType(assetDetails.getLiabilityType());
		asset.setPaymentFrequency(assetDetails.getPaymentFrequency());
		asset.setPaymentAmount(assetDetails.getPaymentAmount());

		if (!willBeActive) {
			asset.setIsFavorite(false);
		} else if (Boolean.TRUE.equals(assetDetails.getIsFavorite())) {
			asset.setIsFavorite(true);
		} else {
			asset.setIsFavorite(false);
		}

		Asset updated = assetRepository.save(Objects.requireNonNull(asset));

		if (wasActive != willBeActive) {
			AssetStatusLog statusLog = new AssetStatusLog();
			statusLog.setAsset(updated);
			statusLog.setChangedAt(LocalDate.now());
			statusLog.setIsActive(willBeActive);
			statusLogRepository.save(statusLog);
			log.debug(
				"AssetService#update - Status change logged: assetId={}, isActive={}",
				updated.getId(),
				willBeActive
			);
		}
		log.info(
			"AssetService#update - Asset updated: assetId={}, name={}",
			updated.getId(),
			updated.getName()
		);
		return updated;
	}

	@Transactional
	public Asset patchStatus(UUID id, boolean isActive, LocalDate changedAt, UUID userId) {
		log.debug(
			"AssetService#patchStatus - Patching status of assetId={} for userId={}, isActive={}, changedAt={}",
			id,
			userId,
			isActive,
			changedAt
		);
		Asset asset = findById(id, userId);
		LocalDate effectiveDate = changedAt != null ? changedAt : LocalDate.now();

		AssetStatusLog statusLog = new AssetStatusLog();
		statusLog.setAsset(asset);
		statusLog.setChangedAt(effectiveDate);
		statusLog.setIsActive(isActive);
		statusLogRepository.save(statusLog);
		log.debug(
			"AssetService#patchStatus - Status log entry created: assetId={}, isActive={}, changedAt={}",
			id,
			isActive,
			effectiveDate
		);

		// Recompute isActive from the latest log entry by date
		boolean newIsActive = statusLogRepository
			.findTopByAssetIdOrderByChangedAtDesc(asset.getId())
			.map(AssetStatusLog::getIsActive)
			.orElse(isActive);

		asset.setIsActive(newIsActive);
		if (!newIsActive) {
			asset.setIsFavorite(false);
		}

		Asset updated = assetRepository.save(asset);
		log.info(
			"AssetService#patchStatus - Asset status updated: assetId={}, isActive={}",
			updated.getId(),
			newIsActive
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
