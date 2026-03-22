package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.analytics.validation.TargetValidationService;
import com.backend.aurum.domain.user.model.UserPrincipal;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.math.BigDecimal;
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

	private final TargetService targetService;
	private final AnalyticsService analyticsService;
	private final TargetValidationService validationService;
	private final TargetMapper mapper;

	@GetMapping
	public ResponseEntity<List<TargetDTO>> getAllTargets(
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.debug("TargetController#getAllTargets - Request to list targets for userId={}", userId);
		BigDecimal netWorth = analyticsService.getSummary(userId).getTotalNetWorth();
		List<TargetDTO> targets = targetService
			.findAll(userId, netWorth)
			.stream()
			.map(t -> mapper.toDto(t, netWorth))
			.toList();
		return ResponseEntity.ok(targets);
	}

	@PostMapping
	public ResponseEntity<TargetDTO> createTarget(
		@RequestBody TargetDTO targetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info("TargetController#createTarget - Request to create target for userId={}", userId);
		validationService.validate(targetDto);
		Target target = mapper.toEntity(targetDto, userId);
		BigDecimal netWorth = analyticsService.getSummary(userId).getTotalNetWorth();
		Target savedTarget = targetService.save(target, netWorth);
		log.info(
			"TargetController#createTarget - Target created successfully: targetId={}",
			savedTarget.getId()
		);
		return ResponseEntity.ok(mapper.toDto(savedTarget, netWorth));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TargetDTO> updateTarget(
		@PathVariable UUID id,
		@RequestBody TargetDTO targetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		log.info(
			"TargetController#updateTarget - Request to update targetId={} for userId={}",
			id,
			userId
		);
		validationService.validate(targetDto);
		Target targetDetails = mapper.toEntity(targetDto, userId);
		BigDecimal netWorth = analyticsService.getSummary(userId).getTotalNetWorth();
		Target updatedTarget = targetService.update(id, targetDetails, netWorth);
		log.info("TargetController#updateTarget - Target updated successfully: targetId={}", id);
		return ResponseEntity.ok(mapper.toDto(updatedTarget, netWorth));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTarget(@PathVariable UUID id) {
		log.info("TargetController#deleteTarget - Request to delete targetId={}", id);
		targetService.delete(id);
		log.info("TargetController#deleteTarget - Target deleted successfully: targetId={}", id);
		return ResponseEntity.noContent().build();
	}
}
