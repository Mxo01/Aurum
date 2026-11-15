package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetUpdate;
import com.backend.aurum.domain.asset.model.LiabilityConfiguration;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
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
	private final CurrencyConversionService conversionService;

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
		requireLiabilityCategoryMatch(LiabilityConfiguration.of(asset), asset.getCategory());
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
			snapshot.setCurrency(savedAsset.getOriginalCurrency());
			snapshot.setExchangeRateToBase(
				conversionService.rate(
					savedAsset.getOriginalCurrency(),
					targetCurrencyOf(savedAsset),
					referenceDate
				)
			);
			snapshotRepository.save(Objects.requireNonNull(snapshot));
			savedAsset.getSnapshots().add(snapshot);
			log.debug("AssetService#save - Initial snapshot saved for assetId={}", savedAsset.getId());
		}

		return savedAsset;
	}

	@Transactional
	public Asset update(UUID id, AssetUpdate details, UUID userId) {
		log.debug("AssetService#update - Updating assetId={} for userId={}", id, userId);
		Asset asset = findById(id, userId);

		LiabilityConfiguration currentLiability = LiabilityConfiguration.of(asset);
		LiabilityConfiguration liability = details.liability().orElse(currentLiability);
		liability.validate();
		// Checked against the state the update produces, not against the request: a rename must not
		// leave an ASSET category carrying a liabilityType, and a category switch must not smuggle a
		// debt into it.
		requireLiabilityCategoryMatch(liability, details.category().orElseGet(asset::getCategory));
		requireSnapshotWhenAutomatic(asset.getId(), liability);

		// A changed payment rule invalidates the persisted schedule: the next run derives it again
		// instead of paying the old one. A settled liability stays settled.
		boolean paymentScheduleChanged = !currentLiability.equals(liability);

		if (details.name() != null) {
			asset.setName(details.name());
		}
		details.category().ifPresent(asset::setCategory);
		if (details.originalCurrency() != null) {
			asset.setOriginalCurrency(details.originalCurrency());
		}
		if (details.isFavorite() != null) {
			asset.setIsFavorite(details.isFavorite());
		}
		if (details.liability().isPresent()) {
			asset.setLiabilityType(liability.type());
			asset.setPaymentFrequency(liability.frequency());
			asset.setPaymentAmount(liability.amount());
		}

		if (paymentScheduleChanged) {
			asset.setNextDueDate(null);
			asset.setPaymentAnchorDay(null);
		}

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

	private Currency targetCurrencyOf(Asset asset) {
		User user = asset.getUser();
		return user != null ? user.getCurrency() : asset.getOriginalCurrency();
	}

	/**
	 * Runs before the flush, in the same transaction that writes the row, so a rejection leaves
	 * nothing persisted: a rule checked after the commit would keep the inconsistent row alive.
	 */
	private void requireLiabilityCategoryMatch(
		LiabilityConfiguration liability,
		AssetCategory category
	) {
		liability.validateForCategory(category != null ? category.getType() : null);
	}

	private void requireSnapshotWhenAutomatic(UUID assetId, LiabilityConfiguration liability) {
		if (!liability.isAutomatic() || snapshotRepository.existsByAssetId(assetId)) {
			return;
		}
		throw new ValidationException(
			"An AUTOMATIC liability requires at least one snapshot before it can be updated"
		);
	}
}
