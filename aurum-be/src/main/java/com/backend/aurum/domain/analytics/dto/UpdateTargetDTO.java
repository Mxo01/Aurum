package com.backend.aurum.domain.analytics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class UpdateTargetDTO {

	private UUID id;
	private String name;
	private BigDecimal targetAmount;
	private BigDecimal currentAmount;
	private LocalDate deadline;
}
