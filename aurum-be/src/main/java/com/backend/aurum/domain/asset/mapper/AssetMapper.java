package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AssetMapper {

	private final AssetCategoryRepository categoryRepository;
	private final UserRepository userRepository;
	private final SnapshotMapper snapshotMapper;

	public Asset toEntity(AssetDTO dto, UUID userId) {
		if (dto == null)
			return null;
		Asset asset = new Asset();
		asset.setId(dto.getId());
		asset.setName(dto.getName());
		asset.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
		asset.setIsFavorite(dto.getIsFavorite() != null ? dto.getIsFavorite() : false);
		asset.setOriginalCurrency(dto.getOriginalCurrency() != null ? dto.getOriginalCurrency() : Currency.EUR);
		asset.setLiabilityType(dto.getLiabilityType());
		asset.setPaymentFrequency(dto.getPaymentFrequency());
		asset.setPaymentAmount(dto.getPaymentAmount());

		if (userId != null) {
			User user = userRepository.findById(userId)
					.orElseThrow(() -> new RuntimeException("User not found"));
			asset.setUser(user);
		}

		if (dto.getCategoryId() != null) {
			AssetCategory category = categoryRepository.findById(dto.getCategoryId())
					.orElseThrow(() -> new RuntimeException("Category not found"));

			if (category.getUser() != null && (userId == null || !category.getUser().getId().equals(userId))) {
				throw new RuntimeException("Category does not belong to the user");
			}

			asset.setCategory(category);
		}

		return asset;
	}

	public AssetDTO toDto(Asset entity) {
		if (entity == null)
			return null;
		AssetDTO dto = new AssetDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setIsActive(entity.getIsActive());
		dto.setIsFavorite(entity.getIsFavorite());
		dto.setOriginalCurrency(entity.getOriginalCurrency());

		if (entity.getCategory() != null) {
			dto.setCategoryId(entity.getCategory().getId());
			dto.setCategoryName(entity.getCategory().getName());
			dto.setType(entity.getCategory().getType());
		}

		if (entity.getSnapshots() != null) {
			dto.setSnapshots(entity.getSnapshots().stream()
					.map(snapshotMapper::toDto)
					.toList());
		}

		dto.setLiabilityType(entity.getLiabilityType());
		dto.setPaymentFrequency(entity.getPaymentFrequency());
		dto.setPaymentAmount(entity.getPaymentAmount());

		return dto;
	}
}
