package com.backend.aurum.domain.analytics.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VariationDTO {

	private DeltaDTO oneMonth;
	private DeltaDTO sixMonths;
	private DeltaDTO oneYear;
}
