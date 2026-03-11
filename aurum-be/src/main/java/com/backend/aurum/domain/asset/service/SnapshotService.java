package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SnapshotService {

    private final SnapshotRepository snapshotRepository;

    public List<Snapshot> findAll(UUID userId) {
        return snapshotRepository.findByAssetUserId(userId);
    }

    public List<Snapshot> findByAssetId(UUID assetId) {
        return snapshotRepository.findByAssetId(assetId);
    }

    public List<Snapshot> findByDateRange(UUID userId, LocalDate start, LocalDate end) {
        return snapshotRepository.findByAssetUserId(userId).stream()
            .filter(s -> !s.getReferenceDate().isBefore(start) && !s.getReferenceDate().isAfter(end))
            .toList();
    }

    public Optional<Snapshot> findExistingForMonth(UUID assetId, LocalDate referenceDate) {
        LocalDate startOfMonth = referenceDate.withDayOfMonth(1);
        LocalDate endOfMonth = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());
        return snapshotRepository.findFirstByAssetIdAndReferenceDateBetween(assetId, startOfMonth, endOfMonth);
    }

    @Transactional
    public Snapshot save(Snapshot snapshot) {
        return snapshotRepository.save(snapshot);
    }

    @Transactional
    public void delete(UUID id) {
        snapshotRepository.deleteById(id);
    }
}
