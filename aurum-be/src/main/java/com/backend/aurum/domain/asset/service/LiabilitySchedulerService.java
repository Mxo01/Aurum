package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiabilitySchedulerService {

	static final String JOB_NAME = "processAutomaticLiabilityPayments";
	static final String JOB_CRON = "0 0 0 * * *";
	static final String JOB_ZONE = "UTC";

	private final AssetRepository assetRepository;
	private final SnapshotService snapshotService;
	private final ExchangeRateProvider exchangeRateService;
	private final SchedulerRunRecorder schedulerRunRecorder;
	private final Clock clock;

	/**
	 * Announces the registration of the job, so a job that is registered but never fires is
	 * distinguishable from a job that was never registered.
	 */
	@PostConstruct
	void logRegistration() {
		log.info("Scheduled job {} registered: cron '{}' zone {}", JOB_NAME, JOB_CRON, JOB_ZONE);
	}

	/**
	 * Runs daily at midnight UTC to process automatic liability payments.
	 */
	@Scheduled(cron = JOB_CRON, zone = JOB_ZONE)
	@Transactional
	public void processAutomaticLiabilityPayments() {
		UUID runId = schedulerRunRecorder.start(JOB_NAME);
		boolean outcomeDeferred = deferOutcomeToTransaction(runId);
		try {
			log.info("Running automatic liability payment processor...");
			List<Asset> automaticLiabilities = assetRepository.findAllByLiabilityTypeWithSnapshots(
				LiabilityType.AUTOMATIC
			);

			LocalDate today = LocalDate.now(clock);

			int processed = 0;
			int failed = 0;

			for (Asset asset : automaticLiabilities) {
				try {
					processPaymentForAsset(asset, today);
					processed++;
				} catch (Exception e) {
					failed++;
					log.error(
						"Failed to process payment for liability asset {} ({}): {}",
						asset.getId(),
						asset.getName(),
						e.getMessage(),
						e
					);
				}
			}

			log.info(
				"Automatic liability payment processor completed: {} processed, {} failed out of {} total",
				processed,
				failed,
				automaticLiabilities.size()
			);
			if (!outcomeDeferred) {
				schedulerRunRecorder.complete(runId);
			}
		} catch (RuntimeException e) {
			if (!outcomeDeferred) {
				schedulerRunRecorder.fail(runId);
			}
			throw e;
		}
	}

	/**
	 * The terminal status must follow the transaction, not the method body: a batch that fails at
	 * flush or commit returns from the body successfully and only rolls back afterwards, so
	 * closing the run from inside the body would record a COMPLETED run for a lost payment batch.
	 *
	 * @return whether the terminal write was deferred to the transaction's afterCompletion, in
	 *         which case the caller must not close the run itself
	 */
	private boolean deferOutcomeToTransaction(UUID runId) {
		if (!TransactionSynchronizationManager.isSynchronizationActive()) {
			return false;
		}

		TransactionSynchronizationManager.registerSynchronization(
			new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					if (status == STATUS_COMMITTED) {
						schedulerRunRecorder.complete(runId);
					} else {
						schedulerRunRecorder.fail(runId);
					}
				}
			}
		);
		return true;
	}

	private void processPaymentForAsset(Asset asset, LocalDate today) {
		if (asset.getPaymentFrequency() == null || asset.getPaymentAmount() == null) {
			return;
		}

		List<Snapshot> snapshots = asset.getSnapshots();
		if (snapshots == null || snapshots.isEmpty()) {
			return;
		}

		Optional<Snapshot> latestSnapshotOpt = snapshots
			.stream()
			.max(Comparator.comparing(Snapshot::getReferenceDate));

		if (latestSnapshotOpt.isEmpty()) {
			return;
		}

		Snapshot latestSnapshot = latestSnapshotOpt.get();
		LocalDate lastPaymentDate = latestSnapshot.getReferenceDate();

		if (!isPaymentDue(lastPaymentDate, today, asset.getPaymentFrequency())) {
			return;
		}

		BigDecimal currentValue = latestSnapshot.getAmountOriginalCurrency();

		// Skip assets that are already fully paid off
		if (currentValue.compareTo(BigDecimal.ZERO) <= 0) {
			return;
		}

		BigDecimal newValue = currentValue.subtract(asset.getPaymentAmount());

		// Do not go below zero
		if (newValue.compareTo(BigDecimal.ZERO) < 0) {
			newValue = BigDecimal.ZERO;
		}

		// Resolve exchange rate
		BigDecimal exchangeRate = BigDecimal.ONE;
		String assetCurrency = asset.getOriginalCurrency().getValue();
		String userCurrency = asset.getUser().getCurrency().getValue();
		if (!assetCurrency.equals(userCurrency)) {
			exchangeRate = exchangeRateService.getRate(assetCurrency, userCurrency, today);
		}

		// Use saveOrUpdate for idempotency — if a snapshot already exists for this month, update it
		snapshotService.saveOrUpdate(asset, newValue, today, exchangeRate);
		log.info(
			"Processed automatic payment snapshot for liability {} (new value: {})",
			asset.getId(),
			newValue
		);

		if (newValue.compareTo(BigDecimal.ZERO) == 0) {
			log.info("Liability {} has reached zero value", asset.getId());
		}
	}

	private boolean isPaymentDue(LocalDate lastDate, LocalDate today, PaymentFrequency frequency) {
		return switch (frequency) {
			case WEEKLY -> !today.isBefore(lastDate.plusWeeks(1));
			case MONTHLY -> !today.isBefore(lastDate.plusMonths(1));
			case YEARLY -> !today.isBefore(lastDate.plusYears(1));
		};
	}
}
