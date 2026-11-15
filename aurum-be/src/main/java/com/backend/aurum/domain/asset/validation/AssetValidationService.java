package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.LiabilityConfiguration;
import com.backend.aurum.domain.shared.exception.ValidationException;
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
			throw new IllegalArgumentException("Asset data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Asset name is required");
		}
		if (dto.getName().length() > 100) {
			throw new IllegalArgumentException("Asset name must not exceed 100 characters");
		}
		if (dto.getCategoryId() == null) {
			throw new IllegalArgumentException("Asset category is required");
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
			throw new IllegalArgumentException("Asset data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Asset name is required");
		}
		if (dto.getName().length() > 100) {
			throw new IllegalArgumentException("Asset name must not exceed 100 characters");
		}

		new LiabilityConfiguration(
			dto.getLiabilityType(),
			dto.getPaymentFrequency(),
			dto.getPaymentAmount()
		).validate();
	}

	/**
	 * An AUTOMATIC liability without a snapshot is skipped by every scheduler run, silently and
	 * forever, so the opening balance is part of the configuration it must be created with.
	 */
	private void validateOpeningSnapshot(CreateAssetDTO dto, LiabilityConfiguration liability) {
		if (!liability.isAutomatic()) {
			return;
		}

		if (dto.getInitialValue() == null || dto.getInitialValue().signum() <= 0) {
			throw new ValidationException(
				"An AUTOMATIC liability requires an initialValue greater than 0"
			);
		}

		if (dto.getReferenceDate() == null) {
			throw new ValidationException("An AUTOMATIC liability requires a referenceDate");
		}

		if (ReferenceDatePolicy.isTooFarInTheFuture(dto.getReferenceDate(), clock)) {
			throw new ValidationException("referenceDate cannot be in the future");
		}
	}
}
