package com.backend.aurum.domain.asset.mapper;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
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

	public AssetCategory toEntity(AssetCategoryDTO dto, UUID userId) {
		if (dto == null) return null;
		AssetCategory category = new AssetCategory();
		category.setId(dto.getId());
		category.setName(dto.getName());
		category.setType(dto.getType());

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
		return dto;
	}
}
