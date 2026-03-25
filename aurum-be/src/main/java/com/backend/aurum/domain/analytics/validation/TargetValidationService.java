package com.backend.aurum.domain.analytics.validation;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TargetValidationService {

	public void validate(CreateTargetDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Target data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Target name is required");
		}
		if (dto.getTargetAmount() == null || dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Target amount must be positive");
		}
	}

	public void validate(UpdateTargetDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Target data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Target name is required");
		}
		if (dto.getTargetAmount() == null || dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Target amount must be positive");
		}
	}
}
