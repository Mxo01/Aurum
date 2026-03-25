package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.facade.TargetFacade;
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
@RequestMapping("/targets")
@RequiredArgsConstructor
@Tag(name = "Targets", description = "Financial goals tracking")
public class TargetController {

	private final TargetFacade targetFacade;

	@GetMapping
	public ResponseEntity<List<TargetDTO>> getAllTargets(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug("TargetController#getAllTargets - Request to list targets for userId={}", userId);
		return ResponseEntity.ok(targetFacade.getAllTargets(userId));
	}

	@PostMapping
	public ResponseEntity<TargetDTO> createTarget(
		@RequestBody CreateTargetDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info("TargetController#createTarget - Request to create target for userId={}", userId);
		TargetDTO result = targetFacade.createTarget(dto, userId);
		log.info(
			"TargetController#createTarget - Target created successfully: targetId={}",
			result.getId()
		);
		return ResponseEntity.ok(result);
	}

	@PutMapping("/{id}")
	public ResponseEntity<TargetDTO> updateTarget(
		@PathVariable UUID id,
		@RequestBody UpdateTargetDTO dto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"TargetController#updateTarget - Request to update targetId={} for userId={}",
			id,
			userId
		);
		TargetDTO result = targetFacade.updateTarget(id, dto, userId);
		log.info("TargetController#updateTarget - Target updated successfully: targetId={}", id);
		return ResponseEntity.ok(result);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTarget(@PathVariable UUID id) {
		log.info("TargetController#deleteTarget - Request to delete targetId={}", id);
		targetFacade.deleteTarget(id);
		log.info("TargetController#deleteTarget - Target deleted successfully: targetId={}", id);
		return ResponseEntity.noContent().build();
	}
}
