package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SnapshotMapper {

    private final AssetRepository assetRepository;

    public Snapshot toEntity(SnapshotDTO dto) {
        if (dto == null) return null;
        Snapshot snapshot = new Snapshot();
        snapshot.setId(dto.getId());
        snapshot.setReferenceDate(dto.getReferenceDate());
        snapshot.setAmountOriginalCurrency(dto.getAmountOriginalCurrency());
        snapshot.setExchangeRateToBase(dto.getExchangeRateToBase() != null ? dto.getExchangeRateToBase() : BigDecimal.ONE);
        
        if (dto.getAssetId() != null) {
            Asset asset = assetRepository.findById(dto.getAssetId())
                .orElseThrow(() -> new RuntimeException("Asset not found"));
            snapshot.setAsset(asset);
        }
        
        return snapshot;
    }

    public SnapshotDTO toDto(Snapshot entity) {
        if (entity == null) return null;
        SnapshotDTO dto = new SnapshotDTO();
        dto.setId(entity.getId());
        dto.setReferenceDate(entity.getReferenceDate());
        dto.setAmountOriginalCurrency(entity.getAmountOriginalCurrency());
        dto.setExchangeRateToBase(entity.getExchangeRateToBase());
        dto.setAmountBaseCurrency(entity.getAmountInBaseCurrency());
        
        if (entity.getAsset() != null) {
            dto.setAssetId(entity.getAsset().getId());
            dto.setAssetName(entity.getAsset().getName());
        }
        
        return dto;
    }
}
