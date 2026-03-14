package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.validation.AssetCategoryValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Asset classification management")
public class AssetCategoryController {

	private final AssetCategoryService categoryService;
	private final AssetCategoryValidationService validationService;
	private final AssetCategoryMapper mapper;

	@GetMapping
	public ResponseEntity<List<AssetCategoryDTO>> getAllCategories(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		List<AssetCategoryDTO> categories = categoryService.findAll(userId).stream()
				.map(mapper::toDto)
				.toList();
		return ResponseEntity.ok(categories);
	}

	@GetMapping("/user")
	public ResponseEntity<List<AssetCategoryDTO>> getUserAssetCategories(
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		List<AssetCategoryDTO> categories = categoryService.findByUserId(userId).stream()
				.map(mapper::toDto)
				.toList();
		return ResponseEntity.ok(categories);
	}

	@PostMapping
	public ResponseEntity<AssetCategoryDTO> createCategory(@RequestBody AssetCategoryDTO categoryDto,
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		validationService.validate(categoryDto);
		AssetCategory category = mapper.toEntity(categoryDto, userId);
		AssetCategory savedCategory = categoryService.save(category);
		return ResponseEntity.ok(mapper.toDto(savedCategory));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AssetCategoryDTO> updateCategory(
			@PathVariable UUID id,
			@RequestBody AssetCategoryDTO categoryDto,
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		validationService.validate(categoryDto);
		AssetCategory categoryDetails = mapper.toEntity(categoryDto, userId);
		AssetCategory updatedCategory = categoryService.update(id, categoryDetails);
		return ResponseEntity.ok(mapper.toDto(updatedCategory));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteCategory(@PathVariable UUID id) {
		categoryService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
