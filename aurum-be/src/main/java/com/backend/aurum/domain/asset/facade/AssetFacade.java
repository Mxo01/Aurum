package com.backend.aurum.domain.asset.facade;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.AssetStatusDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AssetFacade {

	private final AssetService assetService;
	private final AssetMapper mapper;
	private final AssetValidationService validationService;
	private final SnapshotRepository snapshotRepository;

	public List<AssetDTO> getAllAssets(UUID userId) {
		List<Asset> assets = assetService.findAll(userId);
		Map<UUID, List<Snapshot>> latestTwoByAsset = snapshotRepository
			.findByAssetUserIdOrderByReferenceDateDesc(userId)
			.stream()
			.collect(Collectors.groupingBy(s -> s.getAsset().getId()))
			.entrySet()
			.stream()
			.collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().stream().limit(2).toList()));
		return assets
			.stream()
			.map(a -> mapper.toDtoLight(a, latestTwoByAsset))
			.toList();
	}

	public AssetDTO getAsset(UUID assetId, UUID userId) {
		return mapper.toDto(assetService.findById(assetId, userId));
	}

	public AssetDTO createAsset(CreateAssetDTO dto, UUID userId) {
		validationService.validate(dto);
		Asset asset = mapper.toEntity(dto, userId);
		Asset saved = assetService.save(asset, dto.getInitialValue(), dto.getReferenceDate());
		return mapper.toDto(saved);
	}

	public AssetDTO updateAsset(UUID id, UpdateAssetDTO dto, UUID userId) {
		validationService.validate(dto);
		Asset assetDetails = mapper.toEntity(dto, userId);
		Asset updated = assetService.update(id, assetDetails, userId);
		List<Snapshot> latestTwo = snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(id);
		return mapper.toDtoLight(updated, latestTwo);
	}

	public AssetDTO patchAssetStatus(UUID id, AssetStatusDTO statusDto, UUID userId) {
		if (statusDto.getIsActive() == null) {
			throw new IllegalArgumentException("isActive is required");
		}
		Asset updated = assetService.patchStatus(
			id,
			statusDto.getIsActive(),
			statusDto.getChangedAt(),
			userId
		);
		List<Snapshot> latestTwo = snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(id);
		return mapper.toDtoLight(updated, latestTwo);
	}

	public void deleteAsset(UUID id, UUID userId) {
		assetService.delete(id, userId);
	}
}
