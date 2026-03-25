package com.backend.aurum.domain.asset.facade;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.validation.AssetCategoryValidationService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetCategoryFacade {

	private final AssetCategoryService categoryService;
	private final AssetCategoryMapper mapper;
	private final AssetCategoryValidationService validationService;

	public List<AssetCategoryDTO> getAllCategories(UUID userId) {
		return categoryService.findAll(userId).stream().map(mapper::toDto).toList();
	}

	public AssetCategoryDTO createCategory(CreateAssetCategoryDTO dto, UUID userId) {
		validationService.validate(dto);
		AssetCategory category = mapper.toEntity(dto, userId);
		return mapper.toDto(categoryService.save(category));
	}

	public AssetCategoryDTO updateCategory(UUID id, UpdateAssetCategoryDTO dto, UUID userId) {
		validationService.validate(dto);
		AssetCategory categoryDetails = mapper.toEntity(dto, userId);
		AssetCategory updated = categoryService.update(id, categoryDetails, userId);
		return mapper.toDto(updated);
	}

	public void deleteCategory(UUID id, UUID userId) {
		categoryService.delete(id, userId);
	}
}
