package com.backend.aurum.domain.asset.controller;

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
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Tag(name = "Snapshots", description = "Asset value snapshots over time")
public class SnapshotController {

	private final SnapshotService snapshotService;
	private final SnapshotValidationService validationService;
	private final SnapshotMapper mapper;
	private final AssetRepository assetRepository;
	private final ExchangeRateService exchangeRateService;

	@GetMapping
	public ResponseEntity<List<SnapshotDTO>> getAllSnapshots(@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		List<SnapshotDTO> snapshots = snapshotService.findAll(userId).stream()
				.map(mapper::toDto)
				.toList();
		return ResponseEntity.ok(snapshots);
	}

	@PostMapping
	public ResponseEntity<SnapshotDTO> createSnapshot(@RequestBody SnapshotDTO snapshotDto,
			@AuthenticationPrincipal UserPrincipal principal) {
		UUID userId = principal.user().getId();
		validationService.validate(snapshotDto, userId);

		if (snapshotDto.getExchangeRateToBase() == null) {
			Asset asset = assetRepository.findById(snapshotDto.getAssetId())
					.orElseThrow(() -> new IllegalArgumentException("Asset not found"));
			String assetCurrency = asset.getOriginalCurrency().getValue();
			String userCurrency = principal.user().getCurrency().getValue();
			if (!assetCurrency.equals(userCurrency)) {
				BigDecimal rate = exchangeRateService.getRate(assetCurrency, userCurrency, snapshotDto.getReferenceDate());
				snapshotDto.setExchangeRateToBase(rate);
			}
		}

		Optional<Snapshot> existingOpt = snapshotService.findExistingForMonth(snapshotDto.getAssetId(),
				snapshotDto.getReferenceDate());

		Snapshot snapshotToSave;

		if (existingOpt.isPresent()) {
			snapshotToSave = existingOpt.get();
			snapshotToSave.setAmountOriginalCurrency(snapshotDto.getAmountOriginalCurrency());
			snapshotToSave.setReferenceDate(snapshotDto.getReferenceDate());
			if (snapshotDto.getExchangeRateToBase() != null) {
				snapshotToSave.setExchangeRateToBase(snapshotDto.getExchangeRateToBase());
			}
		} else {
			snapshotToSave = mapper.toEntity(snapshotDto, userId);
		}

		Snapshot savedSnapshot = snapshotService.save(snapshotToSave);
		return ResponseEntity.ok(mapper.toDto(savedSnapshot));
	}

	@DeleteMapping("/bulk")
	public ResponseEntity<Void> deleteSnapshotsBulk(@RequestParam UUID assetId, @RequestBody List<UUID> ids, @AuthenticationPrincipal UserPrincipal principal) {
		snapshotService.deleteBulk(ids, assetId, principal.user().getId());
		return ResponseEntity.noContent().build();
	}
}
