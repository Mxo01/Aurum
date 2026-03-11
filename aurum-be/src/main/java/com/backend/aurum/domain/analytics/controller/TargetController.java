package com.backend.aurum.domain.analytics.controller;

import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.analytics.validation.TargetValidationService;
import com.backend.aurum.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/targets")
@RequiredArgsConstructor
@Tag(name = "Targets", description = "Financial goals tracking")
public class TargetController {

    private final TargetService targetService;
    private final AnalyticsService analyticsService;
    private final TargetValidationService validationService;
    private final TargetMapper mapper;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<TargetDTO>> getAllTargets() {
        UUID userId = securityUtils.getCurrentUserId();
        BigDecimal currentNetWorth = analyticsService.getSummary(userId).getTotalNetWorth();
        List<TargetDTO> targets = targetService.findAll(userId).stream()
                .map(t -> mapper.toDto(t, currentNetWorth))
                .toList();
        return ResponseEntity.ok(targets);
    }

    @PostMapping
    public ResponseEntity<TargetDTO> createTarget(@RequestBody TargetDTO targetDto) {
        UUID userId = securityUtils.getCurrentUserId();
        validationService.validate(targetDto);
        Target target = mapper.toEntity(targetDto, userId);
        Target savedTarget = targetService.save(target);
        return ResponseEntity.ok(mapper.toDto(savedTarget, BigDecimal.ZERO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TargetDTO> updateTarget(@PathVariable UUID id, @RequestBody TargetDTO targetDto) {
        UUID userId = securityUtils.getCurrentUserId();
        validationService.validate(targetDto);
        Target targetDetails = mapper.toEntity(targetDto, userId);
        Target updatedTarget = targetService.update(id, targetDetails);
        return ResponseEntity.ok(mapper.toDto(updatedTarget, BigDecimal.ZERO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTarget(@PathVariable UUID id) {
        targetService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
