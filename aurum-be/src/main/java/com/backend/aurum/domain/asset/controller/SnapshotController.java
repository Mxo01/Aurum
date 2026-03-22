package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/snapshots")
@RequiredArgsConstructor
@Tag(name = "Snapshots", description = "Asset value snapshots over time")
public class SnapshotController {

	private final SnapshotService snapshotService;
	private final SnapshotValidationService validationService;
	private final SnapshotMapper mapper;
	private final AssetRepository assetRepository;
	private final ExchangeRateService exchangeRateService;

	@GetMapping
	public ResponseEntity<List<SnapshotDTO>> getSnapshots(
		@AuthenticationPrincipal UserPrincipal principal,
		@RequestParam(required = false) UUID assetId
	) {
		UUID userId = principal.user().getId();
		log.debug(
			"SnapshotController#getSnapshots - Request to list snapshots for userId={}, assetId={}",
			userId,
			assetId
		);
		List<SnapshotDTO> snapshots;
		if (assetId != null) {
			snapshots = snapshotService
				.findByAssetId(assetId, userId)
				.stream()
				.map(mapper::toDto)
				.toList();
		} else {
			snapshots = snapshotService.findAll(userId).stream().map(mapper::toDto).toList();
		}
		return ResponseEntity.ok(snapshots);
	}

	@PostMapping
	public ResponseEntity<SnapshotDTO> createSnapshot(
		@RequestBody CreateSnapshotDTO snapshotDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"SnapshotController#createSnapshot - Request to create snapshot for assetId={}, userId={}",
			snapshotDto.getAssetId(),
			userId
		);
		validationService.validate(snapshotDto, userId);

		if (snapshotDto.getExchangeRateToBase() == null) {
			Asset asset = assetRepository
				.findById(Objects.requireNonNull(snapshotDto.getAssetId()))
				.orElseThrow(() -> new IllegalArgumentException("Asset not found"));
			String assetCurrency = asset.getOriginalCurrency().getValue();
			String userCurrency = principal.user().getCurrency().getValue();
			if (!assetCurrency.equals(userCurrency)) {
				BigDecimal rate = exchangeRateService.getRate(
					assetCurrency,
					userCurrency,
					snapshotDto.getReferenceDate()
				);
				snapshotDto.setExchangeRateToBase(rate);
			}
		}

		Optional<Snapshot> existingOpt = snapshotService.findExistingForMonth(
			snapshotDto.getAssetId(),
			snapshotDto.getReferenceDate()
		);

		Snapshot snapshotToSave;

		if (existingOpt.isPresent()) {
			log.debug(
				"SnapshotController#createSnapshot - Updating existing snapshot for this month: snapshotId={}",
				existingOpt.get().getId()
			);
			snapshotToSave = existingOpt.get();
			snapshotToSave.setAmountOriginalCurrency(snapshotDto.getAmountOriginalCurrency());
			snapshotToSave.setReferenceDate(snapshotDto.getReferenceDate());
			if (snapshotDto.getExchangeRateToBase() != null) {
				snapshotToSave.setExchangeRateToBase(snapshotDto.getExchangeRateToBase());
			}
		} else {
			log.debug(
				"SnapshotController#createSnapshot - No existing snapshot for this month, creating a new one"
			);
			snapshotToSave = mapper.toEntity(snapshotDto, userId);
		}

		Snapshot savedSnapshot = snapshotService.save(snapshotToSave);
		log.info(
			"SnapshotController#createSnapshot - Snapshot saved: snapshotId={}",
			savedSnapshot.getId()
		);

		if (
			snapshotDto.getAmountOriginalCurrency() != null &&
			snapshotDto.getAmountOriginalCurrency().compareTo(BigDecimal.ZERO) == 0
		) {
			Asset asset = assetRepository
				.findById(Objects.requireNonNull(snapshotDto.getAssetId()))
				.orElseThrow(() -> new IllegalArgumentException("Asset not found"));
			if (asset.getLiabilityType() != null) {
				log.info(
					"SnapshotController#createSnapshot - Liability asset balance reached zero, marking as inactive: assetId={}",
					asset.getId()
				);
				asset.setIsActive(false);
				assetRepository.save(asset);
			}
		}

		return ResponseEntity.ok(mapper.toDto(savedSnapshot));
	}

	@DeleteMapping("/bulk")
	public ResponseEntity<Void> deleteSnapshotsBulk(
		@RequestParam UUID assetId,
		@RequestBody List<UUID> ids,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		log.info(
			"SnapshotController#deleteSnapshotsBulk - Request to bulk delete {} snapshot(s) for assetId={}, userId={}",
			ids.size(),
			assetId,
			principal.user().getId()
		);
		snapshotService.deleteBulk(ids, assetId, principal.user().getId());
		log.info(
			"SnapshotController#deleteSnapshotsBulk - Bulk delete completed for assetId={}",
			assetId
		);
		return ResponseEntity.noContent().build();
	}
}
