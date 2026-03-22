package com.backend.aurum.domain.asset.validation;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AssetValidationService {

	public void validate(AssetDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Asset data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Asset name is required");
		}
		if (dto.getCategoryId() == null) {
			throw new IllegalArgumentException("Asset category is required");
		}
	}

	public void validateForUpdate(AssetDTO dto) {
		if (dto == null) {
			throw new IllegalArgumentException("Asset data cannot be null");
		}
		if (!StringUtils.hasText(dto.getName())) {
			throw new IllegalArgumentException("Asset name is required");
		}
	}
}
