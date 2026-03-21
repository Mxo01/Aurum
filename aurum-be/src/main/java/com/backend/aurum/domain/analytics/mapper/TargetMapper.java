package com.backend.aurum.domain.analytics.mapper;

import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetStatus;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TargetMapper {

	private final UserRepository userRepository;

	public Target toEntity(TargetDTO dto, UUID userId) {
		if (dto == null) return null;
		Target target = new Target();
		target.setId(dto.getId());

		if (userId != null) {
			User user = userRepository
				.findById(userId)
				.orElseThrow(() -> new RuntimeException("User not found"));
			target.setUser(user);
		}

		target.setName(dto.getName());
		target.setTargetAmount(dto.getTargetAmount());
		target.setCurrentAmount(dto.getCurrentAmount());
		target.setType(dto.getType() != null ? dto.getType() : TargetType.MANUAL);
		target.setDeadline(dto.getDeadline());
		target.setIsCompleted(dto.getIsCompleted() != null ? dto.getIsCompleted() : false);
		return target;
	}

	public TargetDTO toDto(Target entity, BigDecimal currentNetWorth) {
		if (entity == null) return null;
		TargetDTO dto = new TargetDTO();
		dto.setId(entity.getId());
		dto.setName(entity.getName());
		dto.setTargetAmount(entity.getTargetAmount());
		dto.setDeadline(entity.getDeadline());

		TargetType type = entity.getType() != null ? entity.getType() : TargetType.MANUAL;
		dto.setType(type);

		BigDecimal rawAmount = (entity.getType() == TargetType.NET_WORTH)
			? (currentNetWorth != null ? currentNetWorth : BigDecimal.ZERO)
			: (entity.getCurrentAmount() != null ? entity.getCurrentAmount() : BigDecimal.ZERO);

		// NET_WORTH targets floor at zero — negative net worth means no progress, not debt toward target
		BigDecimal currentAmount =
			entity.getType() == TargetType.NET_WORTH ? rawAmount.max(BigDecimal.ZERO) : rawAmount;

		dto.setCurrentAmount(currentAmount);

		if (Boolean.TRUE.equals(entity.getIsCompleted())) {
			dto.setCurrentAmount(currentAmount.max(entity.getTargetAmount()));
			dto.setCompletionPercentage(100.0);
			dto.setIsCompleted(true);
			dto.setStatus(TargetStatus.COMPLETED);
			return dto;
		}

		double percentage = 0.0;
		if (entity.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
			percentage = currentAmount
				.multiply(new BigDecimal("100"))
				.divide(entity.getTargetAmount(), 2, RoundingMode.HALF_UP)
				.doubleValue();
		}

		double cappedPercentage = Math.max(0.0, Math.min(100.0, percentage));
		dto.setCompletionPercentage(cappedPercentage);

		boolean isFinished = cappedPercentage >= 100.0;
		dto.setIsCompleted(isFinished);

		if (isFinished) {
			dto.setStatus(TargetStatus.COMPLETED);
		} else if (entity.getDeadline() != null && entity.getDeadline().isBefore(LocalDate.now())) {
			dto.setStatus(TargetStatus.NOT_REACHED);
		} else {
			dto.setStatus(TargetStatus.IN_PROGRESS);
		}

		return dto;
	}
}
