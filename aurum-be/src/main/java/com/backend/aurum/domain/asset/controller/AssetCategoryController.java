package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.validation.AssetCategoryValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Asset classification management")
public class AssetCategoryController {

	private final AssetCategoryService categoryService;
	private final AssetCategoryValidationService validationService;
	private final AssetCategoryMapper mapper;

	@GetMapping
	public ResponseEntity<List<AssetCategoryDTO>> getAllCategories(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"AssetCategoryController#getAllCategories - Request to list categories for userId={}",
			userId
		);
		List<AssetCategoryDTO> categories = categoryService
			.findAll(userId)
			.stream()
			.map(mapper::toDto)
			.toList();
		return ResponseEntity.ok(categories);
	}

	@PostMapping
	public ResponseEntity<AssetCategoryDTO> createCategory(
		@RequestBody AssetCategoryDTO categoryDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetCategoryController#createCategory - Request to create category for userId={}",
			userId
		);
		validationService.validate(categoryDto);
		AssetCategory category = mapper.toEntity(categoryDto, userId);
		AssetCategory savedCategory = categoryService.save(category);
		log.info(
			"AssetCategoryController#createCategory - Category created: categoryId={}",
			savedCategory.getId()
		);
		return ResponseEntity.ok(mapper.toDto(savedCategory));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AssetCategoryDTO> updateCategory(
		@PathVariable UUID id,
		@RequestBody AssetCategoryDTO categoryDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetCategoryController#updateCategory - Request to update categoryId={} for userId={}",
			id,
			userId
		);
		validationService.validate(categoryDto);
		AssetCategory categoryDetails = mapper.toEntity(categoryDto, userId);
		AssetCategory updatedCategory = categoryService.update(id, categoryDetails, userId);
		log.info(
			"AssetCategoryController#updateCategory - Category updated successfully: categoryId={}",
			id
		);
		return ResponseEntity.ok(mapper.toDto(updatedCategory));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCategory(
		@PathVariable UUID id,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetCategoryController#deleteCategory - Request to delete categoryId={} for userId={}",
			id,
			userId
		);
		categoryService.delete(id, userId);
		log.info(
			"AssetCategoryController#deleteCategory - Category deleted successfully: categoryId={}",
			id
		);
		return ResponseEntity.noContent().build();
	}
}
