package com.backend.aurum.domain.analytics.dto;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnalyticsSummaryDTO {

	private BigDecimal totalNetWorth;
	private BigDecimal totalGrossAssets;
	private VariationDTO variations;
	private VariationDTO assetVariations;
	private Map<String, BigDecimal> assetAllocation;
	private BigDecimal currencyImpact;
	private BigDecimal totalLiabilities;
	private BigDecimal debtToAssetRatio;
	private List<AssetDTO> topAssets;
}
