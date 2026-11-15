package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.shared.validation.AmountRules;
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
			throw new ValidationException(null, "Snapshot data is required");
		}
		if (dto.getAssetId() == null) {
			throw new ValidationException("assetId", "Asset is required");
		}
		if (dto.getReferenceDate() == null) {
			throw new ValidationException("referenceDate", "Value date is required");
		}
		if (dto.getAmountOriginalCurrency() == null) {
			throw new ValidationException("amountOriginalCurrency", "Amount is required");
		}
		AmountRules.requireNonNegative(
			dto.getAmountOriginalCurrency(),
			"amountOriginalCurrency",
			"Amount"
		);
		AmountRules.requireAmount(dto.getAmountOriginalCurrency(), "amountOriginalCurrency", "Amount");
		if (dto.getExchangeRateToBase() != null) {
			AmountRules.requirePositive(
				dto.getExchangeRateToBase(),
				"exchangeRateToBase",
				"Exchange rate"
			);
			AmountRules.requireExchangeRate(
				dto.getExchangeRateToBase(),
				"exchangeRateToBase",
				"Exchange rate"
			);
		}

		Asset asset = assetRepository
			.findById(Objects.requireNonNull(dto.getAssetId()))
			.orElseThrow(() -> new ValidationException("assetId", "Asset not found"));

		requireOwnership(asset, userId);
		requireNoFutureReferenceDateForAutomaticLiability(asset, dto.getReferenceDate());
	}

	public void validate(SnapshotDTO dto, UUID userId) {
		if (dto == null) {
			throw new ValidationException(null, "Snapshot data is required");
		}
		if (dto.getAssetId() == null) {
			throw new ValidationException("assetId", "Asset is required");
		}
		if (dto.getReferenceDate() == null) {
			throw new ValidationException("referenceDate", "Value date is required");
		}
		if (dto.getAmountOriginalCurrency() == null) {
			throw new ValidationException("amountOriginalCurrency", "Amount is required");
		}
		AmountRules.requireNonNegative(
			dto.getAmountOriginalCurrency(),
			"amountOriginalCurrency",
			"Amount"
		);
		AmountRules.requireAmount(dto.getAmountOriginalCurrency(), "amountOriginalCurrency", "Amount");
		if (dto.getExchangeRateToBase() != null) {
			AmountRules.requirePositive(
				dto.getExchangeRateToBase(),
				"exchangeRateToBase",
				"Exchange rate"
			);
			AmountRules.requireExchangeRate(
				dto.getExchangeRateToBase(),
				"exchangeRateToBase",
				"Exchange rate"
			);
		}

		Asset asset = assetRepository
			.findById(Objects.requireNonNull(dto.getAssetId()))
			.orElseThrow(() -> new ValidationException("assetId", "Asset not found"));

		requireOwnership(asset, userId);
		requireNoFutureReferenceDateForAutomaticLiability(asset, dto.getReferenceDate());
	}

	private void requireOwnership(Asset asset, UUID userId) {
		if (
			!asset.getUser().getId().equals(Objects.requireNonNull(userId, "userId must not be null"))
		) {
			throw new AccessDeniedException("This asset does not belong to you");
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
			throw new ValidationException(
				"referenceDate",
				"Value date cannot be in the future for an automatic liability"
			);
		}
	}
}
