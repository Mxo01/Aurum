package com.backend.aurum.domain.analytics.mapper;

import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TargetMapper {

    private final UserRepository userRepository;

    public Target toEntity(TargetDTO dto, UUID userId) {
        if (dto == null) return null;
        Target target = new Target();
        target.setId(dto.getId());
        
        if (userId != null) {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
            target.setUser(user);
        }

        target.setName(dto.getName());
        target.setTargetAmount(dto.getTargetAmount());
        target.setDeadline(dto.getDeadline());
        target.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
        return target;
    }

    public TargetDTO toDto(Target entity, BigDecimal currentAmount) {
        if (entity == null) return null;
        TargetDTO dto = new TargetDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setTargetAmount(entity.getTargetAmount());
        dto.setCurrentAmount(currentAmount);
        dto.setDeadline(entity.getDeadline());
        dto.setIsCompleted(entity.getIsCompleted());
        
        if (entity.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
            dto.setCompletionPercentage(currentAmount.divide(entity.getTargetAmount(), 4, RoundingMode.HALF_UP).doubleValue() * 100);
        } else {
            dto.setCompletionPercentage(0.0);
        }
        
        return dto;
    }
}
