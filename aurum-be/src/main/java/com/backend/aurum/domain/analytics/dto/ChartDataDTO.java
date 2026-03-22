package com.backend.aurum.domain.analytics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChartDataDTO {

	private List<String> labels;
	private List<BigDecimal> totalNetWorth;
	private List<BigDecimal> totalAssetsOnly;
	private Map<String, List<BigDecimal>> favoriteAssetsValues;
	private Map<String, String> favoriteAssetsTypes;
}
