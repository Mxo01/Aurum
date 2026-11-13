package com.backend.aurum.domain.analytics.dto;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(
	requiredProperties = {
		"totalNetWorth",
		"totalGrossAssets",
		"variations",
		"assetVariations",
		"assetAllocation",
		"currencyImpact",
		"totalLiabilities",
		"debtToAssetRatio",
		"topAssets",
		"oldestSnapshotYear"
	}
)
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

	@Schema(nullable = true)
	private Integer oldestSnapshotYear;
}
