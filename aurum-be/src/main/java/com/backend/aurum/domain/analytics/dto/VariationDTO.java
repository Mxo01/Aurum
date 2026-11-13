package com.backend.aurum.domain.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(requiredProperties = { "oneMonth", "sixMonths", "oneYear" })
public class VariationDTO {

	private DeltaDTO oneMonth;
	private DeltaDTO sixMonths;
	private DeltaDTO oneYear;
}
