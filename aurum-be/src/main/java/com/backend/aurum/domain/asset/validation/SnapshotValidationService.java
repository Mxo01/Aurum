package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SnapshotValidationService {

    public void validate(SnapshotDTO dto) {
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
        if (dto.getExchangeRateToBase() != null && dto.getExchangeRateToBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Exchange rate must be positive");
        }
    }
}
