package com.backend.aurum.domain.asset.dto;

import java.time.LocalDate;
import lombok.Data;

@Data
public class AssetStatusDTO {

	private Boolean isActive;
	private LocalDate changedAt;
}
