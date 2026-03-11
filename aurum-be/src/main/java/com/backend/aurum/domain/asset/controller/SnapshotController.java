package com.backend.aurum.domain.asset.controller;

import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.infrastructure.security.SecurityUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snapshots")
@RequiredArgsConstructor
@Tag(name = "Snapshots", description = "Asset value snapshots over time")
public class SnapshotController {

    private final SnapshotService snapshotService;
    private final SnapshotValidationService validationService;
    private final SnapshotMapper mapper;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<SnapshotDTO>> getAllSnapshots() {
        UUID userId = securityUtils.getCurrentUserId();
        List<SnapshotDTO> snapshots = snapshotService.findAll(userId).stream()
            .map(mapper::toDto)
            .toList();
        return ResponseEntity.ok(snapshots);
    }

    @PostMapping
    public ResponseEntity<SnapshotDTO> createSnapshot(@RequestBody SnapshotDTO snapshotDto) {
        UUID userId = securityUtils.getCurrentUserId();
        validationService.validate(snapshotDto);
        Snapshot snapshot = mapper.toEntity(snapshotDto, userId);
        Snapshot savedSnapshot = snapshotService.save(snapshot);
        return ResponseEntity.ok(mapper.toDto(savedSnapshot));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSnapshot(@PathVariable UUID id) {
        snapshotService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
