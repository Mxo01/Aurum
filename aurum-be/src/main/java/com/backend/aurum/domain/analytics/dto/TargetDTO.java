package com.backend.aurum.domain.analytics.dto;

import com.backend.aurum.domain.analytics.model.TargetStatus;
import com.backend.aurum.domain.analytics.model.TargetType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
public class TargetDTO {

	private UUID id;
	private String name;
	private BigDecimal targetAmount;
	private BigDecimal currentAmount;
	private Double completionPercentage;
	private LocalDate deadline;
	private Boolean isCompleted;
	private TargetType type;
	private TargetStatus status;
}
