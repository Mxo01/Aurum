package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.LiabilityConfiguration;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.shared.validation.AmountRules;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AssetValidationService {

	private final Clock clock;

	public void validate(CreateAssetDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Asset data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Asset name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Asset name cannot exceed 100 characters");
		}
		if (dto.getCategoryId() == null) {
			throw new ValidationException("categoryId", "Asset category is required");
		}

		LiabilityConfiguration liability = new LiabilityConfiguration(
			dto.getLiabilityType(),
			dto.getPaymentFrequency(),
			dto.getPaymentAmount()
		);
		liability.validate();
		validateOpeningSnapshot(dto, liability);
	}

	public void validate(UpdateAssetDTO dto) {
		if (dto == null) {
			throw new ValidationException(null, "Asset data is required");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new ValidationException("name", "Asset name is required");
		}
		if (dto.getName().length() > 100) {
			throw new ValidationException("name", "Asset name cannot exceed 100 characters");
		}

		new LiabilityConfiguration(
			dto.getLiabilityType(),
			dto.getPaymentFrequency(),
			dto.getPaymentAmount()
		).validate();
	}

	/**
	 * An AUTOMATIC liability without a snapshot is skipped by every scheduler run, silently and
	 * forever, so the opening balance is part of the configuration it must be created with. A manual
	 * asset writes the same value as an opening snapshot, under the same column, so the amount rules
	 * apply to both.
	 */
	private void validateOpeningSnapshot(CreateAssetDTO dto, LiabilityConfiguration liability) {
		if (liability.isAutomatic()) {
			validateAutomaticOpeningSnapshot(dto);
			AmountRules.requireAmount(dto.getInitialValue(), "initialValue", "Amount");
			return;
		}

		if (dto.getInitialValue() == null) {
			return;
		}

		AmountRules.requireNonNegative(dto.getInitialValue(), "initialValue", "Amount");
		AmountRules.requireAmount(dto.getInitialValue(), "initialValue", "Amount");
	}

	private void validateAutomaticOpeningSnapshot(CreateAssetDTO dto) {
		if (dto.getInitialValue() == null || dto.getInitialValue().signum() <= 0) {
			throw new ValidationException(
				"initialValue",
				"An automatic liability needs an initial value greater than zero"
			);
		}

		if (dto.getReferenceDate() == null) {
			throw new ValidationException("referenceDate", "An automatic liability needs a value date");
		}

		if (ReferenceDatePolicy.isTooFarInTheFuture(dto.getReferenceDate(), clock)) {
			throw new ValidationException("referenceDate", "Value date cannot be in the future");
		}
	}
}
