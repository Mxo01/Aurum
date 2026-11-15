package com.backend.aurum.domain.analytics.validation;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TargetValidationService {

	public void validate(CreateTargetDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Target data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Target name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Target name cannot exceed 100 characters");
		}
		if (dto.getTargetAmount() == null || dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ValidationException("targetAmount", "Target amount must be greater than zero");
		}
	}

	public void validate(UpdateTargetDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Target data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Target name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Target name cannot exceed 100 characters");
		}
		if (dto.getTargetAmount() == null || dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new ValidationException("targetAmount", "Target amount must be greater than zero");
		}
	}
}
