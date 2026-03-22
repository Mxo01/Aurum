package com.backend.aurum.domain.analytics.service;

import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.model.TargetType;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TargetService {

	private final TargetRepository targetRepository;

	public List<Target> findAll(UUID userId, BigDecimal netWorth) {
		List<Target> targets = targetRepository.findByUserId(userId);
		targets.forEach(t -> checkAndSyncCompletion(t, netWorth));
		return targets;
	}

	public Target findById(UUID id, BigDecimal netWorth) {
		Target target = targetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> new RuntimeException("Target not found"));
		checkAndSyncCompletion(target, netWorth);
		return target;
	}

	@Transactional
	public Target save(Target target, BigDecimal netWorth) {
		checkAndSyncCompletion(target, netWorth);
		return targetRepository.save(target);
	}

	@Transactional
	public Target update(UUID id, Target targetDetails, BigDecimal netWorth) {
		Target target = targetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> new RuntimeException("Target not found"));

		target.setName(targetDetails.getName());
		target.setTargetAmount(targetDetails.getTargetAmount());
		target.setCurrentAmount(targetDetails.getCurrentAmount());
		target.setDeadline(targetDetails.getDeadline());
		// Type is immutable after creation

		checkAndSyncCompletion(target, netWorth);

		return targetRepository.save(target);
	}

	@Transactional
	public void delete(UUID id) {
		targetRepository.deleteById(Objects.requireNonNull(id));
	}

	private void checkAndSyncCompletion(Target target, BigDecimal netWorth) {
		if (Boolean.TRUE.equals(target.getIsCompleted())) {
			return;
		}

		BigDecimal currentAmount =
			target.getType() == TargetType.NET_WORTH
				? netWorth
				: (target.getCurrentAmount() != null ? target.getCurrentAmount() : BigDecimal.ZERO);

		if (target.getTargetAmount().compareTo(BigDecimal.ZERO) > 0) {
			BigDecimal percentage = currentAmount
				.multiply(new BigDecimal("100"))
				.divide(target.getTargetAmount(), 2, RoundingMode.HALF_UP);

			if (percentage.compareTo(new BigDecimal("100")) >= 0) {
				target.setIsCompleted(true);
			}
		}
	}
}
