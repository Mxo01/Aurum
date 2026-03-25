package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
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

	private final AssetCategoryFacade categoryFacade;

	@GetMapping
	public ResponseEntity<List<AssetCategoryDTO>> getAllCategories(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"AssetCategoryController#getAllCategories - Request to list categories for userId={}",
			userId
		);
		return ResponseEntity.ok(categoryFacade.getAllCategories(userId));
	}

	@PostMapping
	public ResponseEntity<AssetCategoryDTO> createCategory(
		@RequestBody CreateAssetCategoryDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetCategoryController#createCategory - Request to create category for userId={}",
			userId
		);
		AssetCategoryDTO result = categoryFacade.createCategory(dto, userId);
		log.info(
			"AssetCategoryController#createCategory - Category created: categoryId={}",
			result.getId()
		);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/{id}")
	public ResponseEntity<AssetCategoryDTO> updateCategory(
		@PathVariable UUID id,
		@RequestBody UpdateAssetCategoryDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetCategoryController#updateCategory - Request to update categoryId={} for userId={}",
			id,
			userId
		);
		AssetCategoryDTO result = categoryFacade.updateCategory(id, dto, userId);
		log.info(
			"AssetCategoryController#updateCategory - Category updated successfully: categoryId={}",
			id
		);
		return ResponseEntity.ok(result);
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
		categoryFacade.deleteCategory(id, userId);
		log.info(
			"AssetCategoryController#deleteCategory - Category deleted successfully: categoryId={}",
			id
		);
		return ResponseEntity.noContent().build();
	}
}
