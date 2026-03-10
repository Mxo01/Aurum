package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;

    public List<Target> findAll(UUID userId) {
        return targetRepository.findByUserId(userId);
    }

    public Target findById(UUID id) {
        return targetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Target not found"));
    }

    @Transactional
    public Target save(Target target) {
        return targetRepository.save(target);
    }

    @Transactional
    public Target update(UUID id, Target targetDetails) {
        Target target = findById(id);
        target.setName(targetDetails.getName());
        target.setTargetAmount(targetDetails.getTargetAmount());
        target.setDeadline(targetDetails.getDeadline());
        target.setIsCompleted(targetDetails.getIsCompleted());
        return targetRepository.save(target);
    }

    @Transactional
    public void delete(UUID id) {
        targetRepository.deleteById(id);
    }
}
