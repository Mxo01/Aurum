package com.backend.aurum.domain.analytics.dto;

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
		"labels", "totalNetWorth", "totalAssetsOnly", "favoriteAssetsValues", "favoriteAssetsTypes"
	}
)
public class ChartDataDTO {

	private List<String> labels;
	private List<BigDecimal> totalNetWorth;
	private List<BigDecimal> totalAssetsOnly;
	private Map<String, List<BigDecimal>> favoriteAssetsValues;
	private Map<String, String> favoriteAssetsTypes;
}
