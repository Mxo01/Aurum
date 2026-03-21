package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
		AnalyticsSummaryDTO summary = analyticsService.getSummary(userId);
		BigDecimal netWorth = summary.getTotalNetWorth();
		BigDecimal grossAssets = summary.getTotalGrossAssets();
		List<TargetDTO> targets = targetService
			.findAll(userId, grossAssets, netWorth)
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
		validationService.validate(targetDto);
		Target target = mapper.toEntity(targetDto, userId);
		AnalyticsSummaryDTO summary = analyticsService.getSummary(userId);
		BigDecimal netWorth = summary.getTotalNetWorth();
		BigDecimal grossAssets = summary.getTotalGrossAssets();
		Target savedTarget = targetService.save(target, grossAssets, netWorth);
		return ResponseEntity.ok(mapper.toDto(savedTarget, netWorth));
	}

	@PutMapping("/{id}")
	public ResponseEntity<TargetDTO> updateTarget(
		@PathVariable UUID id,
		@RequestBody TargetDTO targetDto,
		@AuthenticationPrincipal UserPrincipal principal
	) {
		UUID userId = principal.user().getId();
		validationService.validate(targetDto);
		Target targetDetails = mapper.toEntity(targetDto, userId);
		AnalyticsSummaryDTO summary = analyticsService.getSummary(userId);
		BigDecimal netWorth = summary.getTotalNetWorth();
		BigDecimal grossAssets = summary.getTotalGrossAssets();
		Target updatedTarget = targetService.update(id, targetDetails, grossAssets, netWorth);
		return ResponseEntity.ok(mapper.toDto(updatedTarget, netWorth));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteTarget(@PathVariable UUID id) {
		targetService.delete(id);
		return ResponseEntity.noContent().build();
	}
}
