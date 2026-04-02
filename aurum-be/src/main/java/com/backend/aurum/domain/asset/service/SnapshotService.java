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
	public Snapshot findById(UUID id, UUID userId) {
		log.debug("SnapshotService#findById - Looking up snapshotId={} for userId={}", id, userId);
		Snapshot snapshot = snapshotRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("SnapshotService#findById - Snapshot not found: snapshotId={}", id);
				return new NotFoundException("Snapshot not found");
			});

		if (!snapshot.getAsset().getUser().getId().equals(userId)) {
			log.warn(
				"SnapshotService#findById - Access denied: snapshotId={} does not belong to userId={}",
				id,
				userId
			);
			throw new AccessDeniedException("Access denied: Snapshot does not belong to user");
		}
		return snapshot;
	}

	@Transactional(readOnly = true)
	public List<Snapshot> findByDateRange(UUID userId, LocalDate start, LocalDate end) {
		log.debug(
			"SnapshotService#findByDateRange - Fetching snapshots for userId={} between {} and {}",
			userId,
			start,
			end
		);
		return snapshotRepository.findByAssetUserIdAndReferenceDateBetween(userId, start, end);
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
		return snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(
			assetId,
			startOfMonth,
			endOfMonth
		);
	}

	@Transactional
	public Snapshot save(Snapshot snapshot) {
		log.debug(
			"SnapshotService#save - Saving snapshot for assetId={}, date={}, amount={}",
			snapshot.getAsset() != null ? snapshot.getAsset().getId() : "unknown",
			snapshot.getReferenceDate(),
			snapshot.getAmountOriginalCurrency()
		);
		Snapshot saved = snapshotRepository.save(Objects.requireNonNull(snapshot));
		log.info("SnapshotService#save - Snapshot saved: snapshotId={}", saved.getId());
		return saved;
	}

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
		Snapshot snapshot = existing.orElseGet(Snapshot::new);
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setReferenceDate(referenceDate);
		snapshot.setExchangeRateToBase(exchangeRate);
		Snapshot saved = snapshotRepository.save(Objects.requireNonNull(snapshot));
		log.info("SnapshotService#saveOrUpdate - Snapshot persisted: snapshotId={}", saved.getId());
		return saved;
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		log.debug("SnapshotService#delete - Deleting snapshotId={} for userId={}", id, userId);
		Snapshot snapshot = findById(id, userId);
		snapshotRepository.delete(Objects.requireNonNull(snapshot));
		log.info("SnapshotService#delete - Snapshot deleted: snapshotId={}", id);
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
}
