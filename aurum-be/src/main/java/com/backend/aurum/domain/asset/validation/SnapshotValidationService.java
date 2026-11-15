package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotValidationService {

	private final AssetRepository assetRepository;
	private final Clock clock;

	public void validate(CreateSnapshotDTO dto, UUID userId) {
		if (dto == null) {
			throw new IllegalArgumentException("Snapshot data cannot be null");
		}
		if (dto.getAssetId() == null) {
			throw new IllegalArgumentException("Asset ID is required");
		}
		if (dto.getReferenceDate() == null) {
			throw new IllegalArgumentException("Reference date is required");
		}
		if (dto.getAmountOriginalCurrency() == null) {
			throw new IllegalArgumentException("Amount is required");
		}
		if (dto.getAmountOriginalCurrency().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Amount must be non-negative");
		}
		if (
			dto.getExchangeRateToBase() != null &&
			dto.getExchangeRateToBase().compareTo(BigDecimal.ZERO) <= 0
		) {
			throw new IllegalArgumentException("Exchange rate must be positive");
		}

		Asset asset = assetRepository
			.findById(Objects.requireNonNull(dto.getAssetId()))
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"));

		requireOwnership(asset, userId);
		requireNoFutureReferenceDateForAutomaticLiability(asset, dto.getReferenceDate());
	}

	public void validate(SnapshotDTO dto, UUID userId) {
		if (dto == null) {
			throw new IllegalArgumentException("Snapshot data cannot be null");
		}
		if (dto.getAssetId() == null) {
			throw new IllegalArgumentException("Asset ID is required");
		}
		if (dto.getReferenceDate() == null) {
			throw new IllegalArgumentException("Reference date is required");
		}
		if (dto.getAmountOriginalCurrency() == null) {
			throw new IllegalArgumentException("Amount is required");
		}
		if (dto.getAmountOriginalCurrency().compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("Amount must be non-negative");
		}
		if (
			dto.getExchangeRateToBase() != null &&
			dto.getExchangeRateToBase().compareTo(BigDecimal.ZERO) <= 0
		) {
			throw new IllegalArgumentException("Exchange rate must be positive");
		}

		Asset asset = assetRepository
			.findById(Objects.requireNonNull(dto.getAssetId()))
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"));

		requireOwnership(asset, userId);
		requireNoFutureReferenceDateForAutomaticLiability(asset, dto.getReferenceDate());
	}

	private void requireOwnership(Asset asset, UUID userId) {
		if (
			!asset.getUser().getId().equals(Objects.requireNonNull(userId, "userId must not be null"))
		) {
			throw new IllegalArgumentException("Asset does not belong to the requesting user");
		}
	}

	/**
	 * An automatic liability dates its payments from its latest snapshot, so a stored valuation in
	 * the future would push every period after it and freeze the liability: the write is refused,
	 * and only for the payment schedule that reads it.
	 */
	private void requireNoFutureReferenceDateForAutomaticLiability(
		Asset asset,
		LocalDate referenceDate
	) {
		if (asset.getLiabilityType() != LiabilityType.AUTOMATIC) {
			return;
		}
		if (ReferenceDatePolicy.isTooFarInTheFuture(referenceDate, clock)) {
			throw new IllegalArgumentException(
				"Reference date cannot be in the future for an automatic liability: its payment schedule is anchored on the latest snapshot, so " +
					referenceDate +
					" would freeze the payments"
			);
		}
	}
}
