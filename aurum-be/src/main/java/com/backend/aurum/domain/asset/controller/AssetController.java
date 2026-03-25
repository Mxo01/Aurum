package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.AssetStatusDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.facade.AssetFacade;
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
@RequestMapping("/assets")
@RequiredArgsConstructor
@Tag(name = "Assets", description = "Management of user assets")
public class AssetController {

	private final AssetFacade assetFacade;

	@GetMapping
	public ResponseEntity<List<AssetDTO>> getAllAssets(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug("AssetController#getAllAssets - Request to list all assets for userId={}", userId);
		return ResponseEntity.ok(assetFacade.getAllAssets(userId));
	}

	@GetMapping("/{id}")
	public ResponseEntity<AssetDTO> getAssetById(
		@PathVariable UUID id,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		log.debug(
			"AssetController#getAssetById - Request to get assetId={} for userId={}",
			id,
			principal.user().getId()
		);
		return ResponseEntity.ok(assetFacade.getAsset(id, principal.user().getId()));
	}

	@PostMapping
	public ResponseEntity<AssetDTO> createAsset(
		@RequestBody CreateAssetDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info("AssetController#createAsset - Request to create asset for userId={}", userId);
		AssetDTO result = assetFacade.createAsset(dto, userId);
		log.info(
			"AssetController#createAsset - Asset created successfully: assetId={}",
			result.getId()
		);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/{id}")
	public ResponseEntity<AssetDTO> updateAsset(
		@PathVariable UUID id,
		@RequestBody UpdateAssetDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetController#updateAsset - Request to update assetId={} for userId={}",
			id,
			userId
		);
		AssetDTO result = assetFacade.updateAsset(id, dto, userId);
		log.info("AssetController#updateAsset - Asset updated successfully: assetId={}", id);
		return ResponseEntity.ok(result);
	}

	@PatchMapping("/{id}/status")
	public ResponseEntity<AssetDTO> patchAssetStatus(
		@PathVariable UUID id,
		@RequestBody AssetStatusDTO statusDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"AssetController#patchAssetStatus - Request to patch status of assetId={} for userId={}, isActive={}",
			id,
			userId,
			statusDto.getIsActive()
		);
		AssetDTO result = assetFacade.patchAssetStatus(id, statusDto, userId);
		log.info(
			"AssetController#patchAssetStatus - Asset status patched successfully: assetId={}",
			id
		);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAsset(
		@PathVariable UUID id,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		log.info(
			"AssetController#deleteAsset - Request to delete assetId={} for userId={}",
			id,
			principal.user().getId()
		);
		assetFacade.deleteAsset(id, principal.user().getId());
		log.info("AssetController#deleteAsset - Asset deleted successfully: assetId={}", id);
		return ResponseEntity.noContent().build();
	}
}
