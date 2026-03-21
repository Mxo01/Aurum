package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapshotValidationService {

	private final AssetRepository assetRepository;

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
		if (
			dto.getExchangeRateToBase() != null &&
			dto.getExchangeRateToBase().compareTo(BigDecimal.ZERO) <= 0
		) {
			throw new IllegalArgumentException("Exchange rate must be positive");
		}

		Asset asset = assetRepository
			.findById(dto.getAssetId())
			.orElseThrow(() -> new IllegalArgumentException("Asset not found"));

		if (userId != null && !asset.getUser().getId().equals(userId)) {
			throw new IllegalArgumentException("Asset does not belong to the requesting user");
		}
	}
}
