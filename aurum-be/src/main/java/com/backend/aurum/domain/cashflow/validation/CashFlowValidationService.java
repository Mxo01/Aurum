package com.backend.aurum.domain.cashflow.validation;

import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.shared.validation.AmountRules;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

/**
 * Cash flow has no bean validation on its write path, and the only guard behind it is the
 * {@code month BETWEEN 1 AND 12} check in the database: an out-of-range month leaves the request as a
 * server error instead of a refused one, and a year has no constraint at all, so it persists and then
 * shows up in the year grid and the account export. Both are refused here, before the write, on every
 * path that reaches the facade — the REST update and the MCP single and bulk tools alike.
 */
@Service
public class CashFlowValidationService {

	private static final int MIN_YEAR = 1900;
	private static final int MAX_YEAR = 2100;
	private static final int MIN_MONTH = 1;
	private static final int MAX_MONTH = 12;

	private static final String YEAR_FIELD = "year";
	private static final String MONTH_FIELD = "month";
	private static final String EARNED_FIELD = "earned";
	private static final String SPENT_FIELD = "spent";

	public void validate(Integer year, Integer month, UpdateCashFlowEntryDTO dto) {
		if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
			throw new ValidationException(YEAR_FIELD, "Year must be between 1900 and 2100");
		}
		// A month absent from a bulk entry is not a month between 1 and 12 either, so it shares the
		// sentence instead of needing a required-field one of its own.
		if (month == null || month < MIN_MONTH || month > MAX_MONTH) {
			throw new ValidationException(MONTH_FIELD, "Month must be between 1 and 12");
		}
		if (dto == null) {
			throw new ValidationException(null, "Cash flow data is required");
		}
		// Both amounts are optional: the update only sets the ones it carries, so an absent one is
		// left as stored rather than treated as zero.
		validateAmount(dto.getEarned(), EARNED_FIELD, "Earned");
		validateAmount(dto.getSpent(), SPENT_FIELD, "Spent");
	}

	private void validateAmount(BigDecimal amount, String field, String label) {
		if (amount == null) {
			return;
		}
		AmountRules.requireNonNegative(amount, field, label);
		AmountRules.requireAmount(amount, field, label);
	}
}
