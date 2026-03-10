package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/assets")
@RequiredArgsConstructor
public class AssetController {

    private final AssetService assetService;
    private final AssetValidationService validationService;
    private final AssetMapper mapper;

    @GetMapping
    public ResponseEntity<List<AssetDTO>> getAllAssets(@RequestHeader("X-User-Id") UUID userId) {
        List<AssetDTO> assets = assetService.findAll(userId).stream()
            .map(mapper::toDto)
            .toList();
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDTO> getAssetById(@PathVariable UUID id) {
        Asset asset = assetService.findById(id);
        return ResponseEntity.ok(mapper.toDto(asset));
    }

    @PostMapping
    public ResponseEntity<AssetDTO> createAsset(@RequestBody AssetDTO assetDto) {
        validationService.validate(assetDto);
        Asset asset = mapper.toEntity(assetDto);
        Asset savedAsset = assetService.save(asset);
        return ResponseEntity.ok(mapper.toDto(savedAsset));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AssetDTO> updateAsset(@PathVariable UUID id, @RequestBody AssetDTO assetDto) {
        validationService.validate(assetDto);
        Asset assetDetails = mapper.toEntity(assetDto);
        Asset updatedAsset = assetService.update(id, assetDetails);
        return ResponseEntity.ok(mapper.toDto(updatedAsset));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAsset(@PathVariable UUID id) {
        assetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
