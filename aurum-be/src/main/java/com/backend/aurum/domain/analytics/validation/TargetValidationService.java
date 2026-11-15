package com.backend.aurum.domain.analytics.validation;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.shared.validation.AmountRules;
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
		validateAmounts(dto.getTargetAmount(), dto.getCurrentAmount());
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
		validateAmounts(dto.getTargetAmount(), dto.getCurrentAmount());
	}

	/**
	 * The current amount drives the completion percentage, so a negative one renders a progress bar
	 * that can never fill; both columns are numeric(19,4), so the same scale and magnitude apply.
	 */
	private void validateAmounts(BigDecimal targetAmount, BigDecimal currentAmount) {
		if (targetAmount == null) {
			throw new ValidationException("targetAmount", "Target amount must be greater than zero");
		}
		AmountRules.requirePositive(targetAmount, "targetAmount", "Target amount");
		AmountRules.requireAmount(targetAmount, "targetAmount", "Target amount");

		if (currentAmount == null) {
			return;
		}
		AmountRules.requireNonNegative(currentAmount, "currentAmount", "Current amount");
		AmountRules.requireAmount(currentAmount, "currentAmount", "Current amount");
	}
}
