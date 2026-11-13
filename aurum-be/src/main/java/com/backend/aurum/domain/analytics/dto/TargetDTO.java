package com.backend.aurum.domain.analytics.dto;

import com.backend.aurum.domain.analytics.model.TargetStatus;
import com.backend.aurum.domain.analytics.model.TargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Data;

@Data
@Schema(
	requiredProperties = {
		"id",
		"name",
		"targetAmount",
		"currentAmount",
		"completionPercentage",
		"deadline",
		"isCompleted",
		"type",
		"status"
	}
)
public class TargetDTO {

	@Schema(nullable = true)
	private UUID id;

	private String name;
	private BigDecimal targetAmount;

	@Schema(nullable = true)
	private BigDecimal currentAmount;

	@Schema(nullable = true)
	private Double completionPercentage;

	private LocalDate deadline;

	@Schema(nullable = true)
	private Boolean isCompleted;

	private TargetType type;

	@Schema(nullable = true)
	private TargetStatus status;
}
