package com.backend.aurum.domain.analytics.facade;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.mapper.TargetMapper;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.analytics.service.TargetService;
import com.backend.aurum.domain.analytics.validation.TargetValidationService;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TargetFacade {

	private final TargetService targetService;
	private final TargetMapper mapper;
	private final TargetValidationService validationService;
	private final AnalyticsService analyticsService;

	public List<TargetDTO> getAllTargets(UUID userId) {
		BigDecimal netWorth = analyticsService.getSummary(userId).getTotalNetWorth();
		return targetService
			.findAll(userId, netWorth)
			.stream()
			.map(t -> mapper.toDto(t, netWorth))
			.toList();
	}

	public TargetDTO createTarget(CreateTargetDTO dto, UUID userId) {
		validationService.validate(dto);
		Target target = mapper.toEntity(dto, userId);
		BigDecimal netWorth = analyticsService.getNetWorth(userId);
		Target saved = targetService.save(target, netWorth);
		return mapper.toDto(saved, netWorth);
	}

	public TargetDTO updateTarget(UUID id, UpdateTargetDTO dto, UUID userId) {
		validationService.validate(dto);
		Target targetDetails = mapper.toEntity(dto, userId);
		BigDecimal netWorth = analyticsService.getNetWorth(userId);
		Target updated = targetService.update(id, targetDetails, netWorth);
		return mapper.toDto(updated, netWorth);
	}

	public void deleteTarget(UUID id) {
		targetService.delete(id);
	}
}
