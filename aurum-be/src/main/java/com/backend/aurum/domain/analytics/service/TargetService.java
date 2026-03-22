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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TargetService {

	private final TargetRepository targetRepository;

	public List<Target> findAll(UUID userId, BigDecimal netWorth) {
		log.debug(
			"TargetService#findAll - Fetching targets for userId={}, currentNetWorth={}",
			userId,
			netWorth
		);
		List<Target> targets = targetRepository.findByUserId(userId);
		log.debug("TargetService#findAll - Found {} target(s) for userId={}", targets.size(), userId);
		targets.forEach(t -> checkAndSyncCompletion(t, netWorth));
		return targets;
	}

	public Target findById(UUID id, BigDecimal netWorth) {
		log.debug("TargetService#findById - Looking up targetId={}", id);
		Target target = targetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("TargetService#findById - Target not found: targetId={}", id);
				return new RuntimeException("Target not found");
			});
		checkAndSyncCompletion(target, netWorth);
		return target;
	}

	@Transactional
	public Target save(Target target, BigDecimal netWorth) {
		log.debug("TargetService#save - Persisting new target: name={}", target.getName());
		checkAndSyncCompletion(target, netWorth);
		Target saved = targetRepository.save(target);
		log.info(
			"TargetService#save - Target created: targetId={}, name={}",
			saved.getId(),
			saved.getName()
		);
		return saved;
	}

	@Transactional
	public Target update(UUID id, Target targetDetails, BigDecimal netWorth) {
		log.debug("TargetService#update - Updating targetId={}", id);
		Target target = targetRepository
			.findById(Objects.requireNonNull(id))
			.orElseThrow(() -> {
				log.warn("TargetService#update - Target not found: targetId={}", id);
				return new RuntimeException("Target not found");
			});

		target.setName(targetDetails.getName());
		target.setTargetAmount(targetDetails.getTargetAmount());
		target.setCurrentAmount(targetDetails.getCurrentAmount());
		target.setDeadline(targetDetails.getDeadline());
		// Type is immutable after creation

		checkAndSyncCompletion(target, netWorth);

		Target updated = targetRepository.save(target);
		log.info(
			"TargetService#update - Target updated: targetId={}, name={}",
			updated.getId(),
			updated.getName()
		);
		return updated;
	}

	@Transactional
	public void delete(UUID id) {
		log.debug("TargetService#delete - Deleting targetId={}", id);
		targetRepository.deleteById(Objects.requireNonNull(id));
		log.info("TargetService#delete - Target deleted: targetId={}", id);
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
				log.info(
					"TargetService#checkAndSyncCompletion - Target marked as completed: targetId={}, name={}",
					target.getId(),
					target.getName()
				);
			}
		}
	}
}
