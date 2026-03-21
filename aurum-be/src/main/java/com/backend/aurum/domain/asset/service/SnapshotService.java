package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SnapshotService {

	private final SnapshotRepository snapshotRepository;
	private final AssetRepository assetRepository;

	@Transactional(readOnly = true)
	public List<Snapshot> findAll(UUID userId) {
		return snapshotRepository.findByAssetUserId(userId);
	}

	@Transactional(readOnly = true)
	public List<Snapshot> findByAssetId(UUID assetId, UUID userId) {
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(assetId))
			.orElseThrow(() -> new RuntimeException("Asset not found"));
		if (!asset.getUser().getId().equals(userId)) {
			throw new RuntimeException("Access denied: Asset does not belong to user");
		}
		return snapshotRepository.findByAssetId(assetId);
	}

	@Transactional(readOnly = true)
	public Snapshot findById(UUID id, UUID userId) {
		Snapshot snapshot = snapshotRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> new RuntimeException("Snapshot not found"));

		if (!snapshot.getAsset().getUser().getId().equals(userId)) {
			throw new RuntimeException("Access denied: Snapshot does not belong to user");
		}
		return snapshot;
	}

	@Transactional(readOnly = true)
	public List<Snapshot> findByDateRange(UUID userId, LocalDate start, LocalDate end) {
		return snapshotRepository.findByAssetUserIdAndReferenceDateBetween(userId, start, end);
	}

	@Transactional(readOnly = true)
	public Optional<Snapshot> findExistingForMonth(UUID assetId, LocalDate referenceDate) {
		LocalDate startOfMonth = referenceDate.withDayOfMonth(1);
		LocalDate endOfMonth = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
		return snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(
			assetId,
			startOfMonth,
			endOfMonth
		);
	}

	@Transactional
	public Snapshot save(Snapshot snapshot) {
		return snapshotRepository.save(Objects.requireNonNull(snapshot));
	}

	@Transactional
	public Snapshot saveOrUpdate(
		Asset asset,
		BigDecimal amount,
		LocalDate referenceDate,
		BigDecimal exchangeRate
	) {
		Optional<Snapshot> existing = findExistingForMonth(asset.getId(), referenceDate);
		Snapshot snapshot = existing.orElseGet(Snapshot::new);
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setReferenceDate(referenceDate);
		snapshot.setExchangeRateToBase(exchangeRate);
		return snapshotRepository.save(Objects.requireNonNull(snapshot));
	}

	@Transactional
	public void delete(UUID id, UUID userId) {
		Snapshot snapshot = findById(id, userId);
		snapshotRepository.delete(Objects.requireNonNull(snapshot));
	}

	@Transactional
	public void deleteBulk(List<UUID> ids, UUID assetId, UUID userId) {
		List<Snapshot> snapshots = snapshotRepository.findAllById(Objects.requireNonNull(ids));

		for (Snapshot s : snapshots) {
			if (!s.getAsset().getId().equals(assetId)) {
				throw new RuntimeException(
					"Snapshot " + s.getId() + " does not belong to asset " + assetId
				);
			}

			if (!s.getAsset().getUser().getId().equals(userId)) {
				throw new RuntimeException(
					"Access denied: Snapshot " + s.getId() + " does not belong to user"
				);
			}
		}

		snapshotRepository.deleteAllInBatch(snapshots);
	}
}
