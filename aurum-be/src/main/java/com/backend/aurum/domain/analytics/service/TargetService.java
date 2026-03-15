package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TargetService {

    private final TargetRepository targetRepository;
    private final AnalyticsService analyticsService;

    public List<Target> findAll(UUID userId) {
        List<Target> targets = targetRepository.findByUserId(userId);
        BigDecimal currentNetWorth = analyticsService.getSummary(userId).getTotalNetWorth();
        
        // Sync completion status for all targets
        targets.forEach(t -> checkAndSyncCompletion(t, currentNetWorth));
        
        return targets;
    }

    public Target findById(UUID id) {
        Target target = targetRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Target not found"));
        
        BigDecimal currentNetWorth = analyticsService.getSummary(target.getUser().getId()).getTotalNetWorth();
        checkAndSyncCompletion(target, currentNetWorth);
        
        return target;
    }

    @Transactional
    public Target save(Target target) {
        BigDecimal currentNetWorth = analyticsService.getSummary(target.getUser().getId()).getTotalNetWorth();
        checkAndSyncCompletion(target, currentNetWorth);
        return targetRepository.save(target);
    }

    @Transactional
    public Target update(UUID id, Target targetDetails) {
        Target target = findById(id);
        
        // If it was already completed, we "freeze" it - we don't allow changing most fields 
        // except maybe name if really needed, but let's allow it for now.
        // However, we strictly don't allow un-completing it if it reached the goal.
        
        target.setName(targetDetails.getName());
        target.setTargetAmount(targetDetails.getTargetAmount());
        target.setCurrentAmount(targetDetails.getCurrentAmount());
        target.setDeadline(targetDetails.getDeadline());
        target.setType(targetDetails.getType());

        BigDecimal currentNetWorth = analyticsService.getSummary(target.getUser().getId()).getTotalNetWorth();
        checkAndSyncCompletion(target, currentNetWorth);
        
        return targetRepository.save(target);
    }

    @Transactional
    public void delete(UUID id) {
        targetRepository.deleteById(id);
    }

    private void checkAndSyncCompletion(Target target, BigDecimal currentNetWorth) {
        if (Boolean.TRUE.equals(target.getIsCompleted())) {
            return;
        }

        BigDecimal currentAmount = target.getType() == TargetType.NET_WORTH
                ? currentNetWorth
                : (target.getCurrentAmount() != null ? target.getCurrentAmount() : BigDecimal.ZERO);

        if (target.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = currentAmount.multiply(new BigDecimal("100"))
                    .divide(target.getTargetAmount(), 2, RoundingMode.HALF_UP);
            
            if (percentage.compareTo(new BigDecimal("100")) >= 0) {
                target.setIsCompleted(true);
            }
        }
    }
}
