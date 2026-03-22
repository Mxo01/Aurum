package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssetCategoryMapper {

	private final UserRepository userRepository;

	public AssetCategory toEntity(CreateAssetCategoryDTO dto, UUID userId) {
		if (dto == null) return null;
		AssetCategory category = new AssetCategory();
		category.setName(dto.getName());
		category.setType(dto.getType());
		category.setIcon(dto.getIcon());

		if (userId != null) {
			User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
			category.setUser(user);
		}

		return category;
	}

	public AssetCategory toEntity(AssetCategoryDTO dto, UUID userId) {
		if (dto == null) return null;
		AssetCategory category = new AssetCategory();
		category.setName(dto.getName());
		category.setType(dto.getType());
		category.setIcon(dto.getIcon());

		if (userId != null) {
			User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
			category.setUser(user);
		}

		return category;
	}

	public AssetCategory toEntity(UpdateAssetCategoryDTO dto, UUID userId) {
		if (dto == null) return null;
		AssetCategory category = new AssetCategory();
		category.setName(dto.getName());
		category.setType(dto.getType());
		category.setIcon(dto.getIcon());

		if (userId != null) {
			User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
			category.setUser(user);
		}

		return category;
	}

	public AssetCategoryDTO toDto(AssetCategory entity) {
		if (entity == null) return null;
		AssetCategoryDTO dto = new AssetCategoryDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setType(entity.getType());
		dto.setIcon(entity.getIcon());
		dto.setDefault(entity.getUser() == null);
		return dto;
	}
}
