package com.backend.aurum.domain.analytics.dto;

import com.backend.aurum.domain.analytics.model.TargetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class CreateTargetDTO {

	private String name;
	private BigDecimal targetAmount;
	private TargetType type;
	private LocalDate deadline;
	private BigDecimal currentAmount;
}
