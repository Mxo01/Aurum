package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Management of user assets")
public class AssetController {

	private final AssetService assetService;
	private final AssetValidationService validationService;
	private final AssetMapper mapper;

	@GetMapping
	public ResponseEntity<List<AssetDTO>> getAllAssets(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		List<AssetDTO> assets = assetService.findAll(userId).stream().map(mapper::toDto).toList();
		return ResponseEntity.ok(assets);
	}

	@GetMapping("/{id}")
	public ResponseEntity<AssetDTO> getAssetById(
		@PathVariable UUID id,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		Asset asset = assetService.findById(id, principal.user().getId());
		return ResponseEntity.ok(mapper.toDto(asset));
	}

	@PostMapping
	public ResponseEntity<AssetDTO> createAsset(
		@RequestBody AssetDTO assetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		validationService.validate(assetDto);
		Asset asset = mapper.toEntity(assetDto, userId);
		Asset savedAsset = assetService.save(
			asset,
			assetDto.getInitialValue(),
			assetDto.getReferenceDate()
		);
		return ResponseEntity.ok(mapper.toDto(savedAsset));
	}

	@PutMapping("/{id}")
	public ResponseEntity<AssetDTO> updateAsset(
		@PathVariable UUID id,
		@RequestBody AssetDTO assetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		validationService.validate(assetDto);
		Asset assetDetails = mapper.toEntity(assetDto, userId);
		Asset updatedAsset = assetService.update(id, assetDetails, userId);
		return ResponseEntity.ok(mapper.toDto(updatedAsset));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAsset(
		@PathVariable UUID id,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		assetService.delete(id, principal.user().getId());
		return ResponseEntity.noContent().build();
	}
}
