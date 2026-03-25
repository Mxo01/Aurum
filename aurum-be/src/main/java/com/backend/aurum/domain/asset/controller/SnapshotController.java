package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
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
@RequestMapping("/snapshots")
@RequiredArgsConstructor
@Tag(name = "Snapshots", description = "Asset value snapshots over time")
public class SnapshotController {

	private final SnapshotFacade snapshotFacade;

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
		List<SnapshotDTO> snapshots =
			assetId != null
				? snapshotFacade.getSnapshots(assetId, userId)
				: snapshotFacade.getAllSnapshots(userId);
		return ResponseEntity.ok(snapshots);
	}

	@PostMapping
	public ResponseEntity<SnapshotDTO> createSnapshot(
		@RequestBody CreateSnapshotDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"SnapshotController#createSnapshot - Request to create snapshot for assetId={}, userId={}",
			dto.getAssetId(),
			userId
		);
		SnapshotDTO result = snapshotFacade.createSnapshot(dto, userId);
		log.info("SnapshotController#createSnapshot - Snapshot saved: snapshotId={}", result.getId());
		return ResponseEntity.ok(result);
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
		snapshotFacade.deleteBulk(assetId, ids, principal.user().getId());
		log.info(
			"SnapshotController#deleteSnapshotsBulk - Bulk delete completed for assetId={}",
			assetId
		);
		return ResponseEntity.noContent().build();
	}
}
