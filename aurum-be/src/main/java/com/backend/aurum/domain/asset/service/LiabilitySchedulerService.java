package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Runs the daily automatic liability payment batch.
 *
 * <p>The batch owns no transaction: it selects ids, hands each one to
 * {@link LiabilityPaymentService}, which pays it in its own transaction. The per-asset log line is
 * written only after that call returned, which is after its commit — a log line never reports a
 * payment that was rolled back.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LiabilitySchedulerService {

	static final String JOB_NAME = "processAutomaticLiabilityPayments";
	static final String JOB_CRON = "0 0 0 * * *";
	static final String JOB_ZONE = "UTC";

	private final AssetRepository assetRepository;
	private final LiabilityPaymentService paymentService;
	private final SchedulerRunRecorder schedulerRunRecorder;
	private final Clock clock;

	/**
	 * The effective cron of the job: the property override when present, the annotation default
	 * otherwise, so the registration log never disagrees with what the scheduler actually runs.
	 */
	@Value("${aurum.liability.scheduler.cron:" + JOB_CRON + "}")
	private String cron;

	/**
	 * Announces the registration of the job, so a job that is registered but never fires is
	 * distinguishable from a job that was never registered.
	 */
	@PostConstruct
	void logRegistration() {
		log.info("Scheduled job {} registered: cron '{}' zone {}", JOB_NAME, cron, JOB_ZONE);
	}

	/**
	 * Runs daily at midnight UTC to process automatic liability payments.
	 */
	@Scheduled(cron = "${aurum.liability.scheduler.cron:" + JOB_CRON + "}", zone = JOB_ZONE)
	public void processAutomaticLiabilityPayments() {
		UUID runId = schedulerRunRecorder.start(JOB_NAME);
		try {
			log.info("Running automatic liability payment processor...");
			LocalDate today = LocalDate.now(clock);
			List<UUID> assetIds = assetRepository.findIdsByLiabilityTypeAndSettledAtIsNull(
				LiabilityType.AUTOMATIC
			);

			int paid = 0;
			int skipped = 0;
			int notDue = 0;
			int failed = 0;

			for (UUID assetId : assetIds) {
				try {
					LiabilityPaymentResult result = paymentService.pay(assetId, today);
					switch (result.status()) {
						case PAID -> {
							paid++;
							logPayment(assetId, result);
						}
						case SKIPPED -> {
							skipped++;
							log.info("Skipped automatic liability {}: {}", assetId, result.reason());
						}
						case NOT_DUE -> {
							notDue++;
							log.debug(
								"Automatic liability {} is not due before {}",
								assetId,
								result.nextDueDate()
							);
						}
					}
				} catch (Exception e) {
					failed++;
					log.error(
						"Failed to process payment for liability asset {}: {}",
						assetId,
						e.getMessage(),
						e
					);
				}
			}

			log.info(
				"Automatic liability payment processor completed: {} paid, {} skipped, {} not due, {} failed out of {} total",
				paid,
				skipped,
				notDue,
				failed,
				assetIds.size()
			);
			closeRun(runId, failed);
		} catch (RuntimeException e) {
			schedulerRunRecorder.fail(runId);
			throw e;
		}
	}

	private void logPayment(UUID assetId, LiabilityPaymentResult result) {
		if (result.isClamped()) {
			log.warn(
				"Automatic liability {} applied {} of the {} configured per period over {} period(s): the configured payment exceeded the outstanding balance and the closing period was clamped",
				assetId,
				result.appliedAmount(),
				result.configuredPaymentAmount(),
				result.paymentsApplied()
			);
		}
		if (result.outstandingBalance().signum() == 0) {
			log.info(
				"Automatic liability {} paid {} period(s) and is settled",
				assetId,
				result.paymentsApplied()
			);
			return;
		}
		log.info(
			"Automatic liability {} paid {} period(s); balance {}, next due {}",
			assetId,
			result.paymentsApplied(),
			result.outstandingBalance(),
			result.nextDueDate()
		);
	}

	private void closeRun(UUID runId, int failed) {
		if (failed > 0) {
			schedulerRunRecorder.fail(runId);
		} else {
			schedulerRunRecorder.complete(runId);
		}
	}
}
