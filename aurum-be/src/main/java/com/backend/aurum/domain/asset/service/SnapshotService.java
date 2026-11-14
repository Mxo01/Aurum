package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnapshotService {

	private final SnapshotRepository snapshotRepository;
	private final AssetRepository assetRepository;

	@Transactional(readOnly = true)
	public List<Snapshot> findAll(UUID userId) {
		log.debug("SnapshotService#findAll - Fetching all snapshots for userId={}", userId);
		List<Snapshot> snapshots = snapshotRepository.findByAssetUserId(userId);
		log.debug(
			"SnapshotService#findAll - Found {} snapshot(s) for userId={}",
			snapshots.size(),
			userId
		);
		return snapshots;
	}

	@Transactional(readOnly = true)
	public List<Snapshot> findByAssetId(UUID assetId, UUID userId) {
		log.debug(
			"SnapshotService#findByAssetId - Fetching snapshots for assetId={}, userId={}",
			assetId,
			userId
		);
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(assetId))
			.orElseThrow(() -> {
				log.warn("SnapshotService#findByAssetId - Asset not found: assetId={}", assetId);
				return new NotFoundException("Asset not found");
			});
		if (!asset.getUser().getId().equals(userId)) {
			log.warn(
				"SnapshotService#findByAssetId - Access denied: assetId={} does not belong to userId={}",
				assetId,
				userId
			);
			throw new AccessDeniedException("Access denied: Asset does not belong to user");
		}
		List<Snapshot> snapshots = snapshotRepository.findByAssetId(assetId);
		log.debug(
			"SnapshotService#findByAssetId - Found {} snapshot(s) for assetId={}",
			snapshots.size(),
			assetId
		);
		return snapshots;
	}

	@Transactional(readOnly = true)
	public Optional<Snapshot> findExistingForMonth(UUID assetId, LocalDate referenceDate) {
		LocalDate startOfMonth = referenceDate.withDayOfMonth(1);
		LocalDate endOfMonth = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
		log.debug(
			"SnapshotService#findExistingForMonth - Checking existing snapshot for assetId={} in month {}/{}",
			assetId,
			referenceDate.getMonthValue(),
			referenceDate.getYear()
		);
		return snapshotRepository.findFirstByAssetIdAndSourceAndReferenceDateBetween(
			assetId,
			SnapshotSource.USER,
			startOfMonth,
			endOfMonth
		);
	}

	/**
	 * The user's own valuation of an asset: it matches user rows only, so a month already paid by
	 * the scheduler keeps its scheduled row untouched.
	 */
	@Transactional
	public Snapshot saveOrUpdate(
		Asset asset,
		BigDecimal amount,
		LocalDate referenceDate,
		BigDecimal exchangeRate
	) {
		log.debug(
			"SnapshotService#saveOrUpdate - Saving or updating snapshot for assetId={}, date={}, amount={}",
			asset.getId(),
			referenceDate,
			amount
		);
		Optional<Snapshot> existing = findExistingForMonth(asset.getId(), referenceDate);
		if (existing.isPresent()) {
			log.debug(
				"SnapshotService#saveOrUpdate - Existing snapshot found (snapshotId={}), updating it",
				existing.get().getId()
			);
		} else {
			log.debug(
				"SnapshotService#saveOrUpdate - No existing snapshot for this month, creating a new one"
			);
		}
		Snapshot snapshot = existing.orElseGet(() -> newSnapshot(asset, SnapshotSource.USER));
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setReferenceDate(referenceDate);
		snapshot.setExchangeRateToBase(exchangeRate);
		Snapshot saved = snapshotRepository.save(Objects.requireNonNull(snapshot));
		log.info("SnapshotService#saveOrUpdate - Snapshot persisted: snapshotId={}", saved.getId());
		return saved;
	}

	/**
	 * Appends one scheduled payment. Insert-only on purpose: the row records what the scheduler paid
	 * on that date, and idempotency is enforced by the source-scoped unique index, not by an upsert.
	 */
	@Transactional
	public Snapshot saveScheduledPayment(
		Asset asset,
		BigDecimal amount,
		LocalDate referenceDate,
		BigDecimal exchangeRate
	) {
		Snapshot snapshot = newSnapshot(asset, SnapshotSource.SCHEDULER);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setReferenceDate(referenceDate);
		snapshot.setExchangeRateToBase(exchangeRate);
		Snapshot saved = snapshotRepository.save(Objects.requireNonNull(snapshot));
		log.debug(
			"SnapshotService#saveScheduledPayment - Scheduled payment persisted: snapshotId={}, assetId={}, date={}, amount={}",
			saved.getId(),
			asset.getId(),
			referenceDate,
			amount
		);
		return saved;
	}

	@Transactional
	public void deleteBulk(List<UUID> ids, UUID assetId, UUID userId) {
		log.debug(
			"SnapshotService#deleteBulk - Bulk deleting {} snapshot(s) for assetId={}, userId={}",
			ids.size(),
			assetId,
			userId
		);
		List<Snapshot> snapshots = snapshotRepository.findAllById(Objects.requireNonNull(ids));

		for (Snapshot s : snapshots) {
			if (!s.getAsset().getId().equals(assetId)) {
				log.warn(
					"SnapshotService#deleteBulk - Snapshot {} does not belong to assetId={}",
					s.getId(),
					assetId
				);
				throw new AccessDeniedException(
					"Snapshot " + s.getId() + " does not belong to asset " + assetId
				);
			}

			if (!s.getAsset().getUser().getId().equals(userId)) {
				log.warn(
					"SnapshotService#deleteBulk - Access denied: snapshotId={} does not belong to userId={}",
					s.getId(),
					userId
				);
				throw new AccessDeniedException(
					"Access denied: Snapshot " + s.getId() + " does not belong to user"
				);
			}
		}

		snapshotRepository.deleteAllInBatch(snapshots);
		log.info(
			"SnapshotService#deleteBulk - Deleted {} snapshot(s) for assetId={}",
			snapshots.size(),
			assetId
		);
	}

	private Snapshot newSnapshot(Asset asset, SnapshotSource source) {
		Snapshot snapshot = new Snapshot();
		snapshot.setAsset(asset);
		snapshot.setSource(source);
		return snapshot;
	}
}
