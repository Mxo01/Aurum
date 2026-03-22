package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
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

	private final AssetService assetService;
	private final AssetValidationService validationService;
	private final AssetMapper mapper;
	private final SnapshotRepository snapshotRepository;

	@GetMapping
	public ResponseEntity<List<AssetDTO>> getAllAssets(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug("AssetController#getAllAssets - Request to list all assets for userId={}", userId);
		List<Asset> assets = assetService.findAll(userId);

		// Single bulk query — load all snapshots ordered DESC, group, keep latest 2 per asset
		Map<UUID, List<Snapshot>> latestTwoByAsset = snapshotRepository
			.findByAssetUserIdOrderByReferenceDateDesc(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()))
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().limit(2).toList()));

		List<AssetDTO> dtos = assets
			.stream()
			.map(a -> mapper.toDtoLight(a, latestTwoByAsset))
			.toList();
		return ResponseEntity.ok(dtos);
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
		Asset asset = assetService.findById(id, principal.user().getId());
		return ResponseEntity.ok(mapper.toDto(asset));
	}

	@PostMapping
	public ResponseEntity<AssetDTO> createAsset(
		@RequestBody AssetDTO assetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info("AssetController#createAsset - Request to create asset for userId={}", userId);
		validationService.validate(assetDto);
		Asset asset = mapper.toEntity(assetDto, userId);
		Asset savedAsset = assetService.save(
			asset,
			assetDto.getInitialValue(),
			assetDto.getReferenceDate()
		);
		log.info(
			"AssetController#createAsset - Asset created successfully: assetId={}",
			savedAsset.getId()
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
		log.info(
			"AssetController#updateAsset - Request to update assetId={} for userId={}",
			id,
			userId
		);
		validationService.validate(assetDto);
		Asset assetDetails = mapper.toEntity(assetDto, userId);
		Asset updatedAsset = assetService.update(id, assetDetails, userId);
		log.info("AssetController#updateAsset - Asset updated successfully: assetId={}", id);
		return ResponseEntity.ok(mapper.toDto(updatedAsset));
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
		assetService.delete(id, principal.user().getId());
		log.info("AssetController#deleteAsset - Asset deleted successfully: assetId={}", id);
		return ResponseEntity.noContent().build();
	}
}
