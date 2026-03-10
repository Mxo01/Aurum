package com.backend.aurum.domain.analytics.validation;

import com.backend.aurum.domain.analytics.dto.TargetDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
public class TargetValidationService {

    public void validate(TargetDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Target data cannot be null");
        }
        if (!StringUtils.hasText(dto.getName())) {
            throw new IllegalArgumentException("Target name is required");
        }
        if (dto.getTargetAmount() == null || dto.getTargetAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Target amount must be positive");
        }
    }
}
