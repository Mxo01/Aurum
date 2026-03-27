package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiabilitySchedulerService {

	private final AssetRepository assetRepository;
	private final SnapshotRepository snapshotRepository;
	private final ExchangeRateService exchangeRateService;

	/**
	 * Runs daily at midnight to process automatic liability payments.
	 */
	@Scheduled(cron = "0 0 0 * * *")
	@Transactional
	public void processAutomaticLiabilityPayments() {
		log.info("Running automatic liability payment processor...");
		List<Asset> automaticLiabilities =
			assetRepository.findAllByIsActiveTrueAndLiabilityTypeWithSnapshots(LiabilityType.AUTOMATIC);

		LocalDate today = LocalDate.now();

		for (Asset asset : automaticLiabilities) {
			try {
				processPaymentForAsset(asset, today);
			} catch (Exception e) {
				log.error(
					"Failed to process payment for liability asset {}: {}",
					asset.getId(),
					e.getMessage()
				);
			}
		}
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

		Snapshot newSnapshot = new Snapshot();
		newSnapshot.setAsset(asset);
		newSnapshot.setAmountOriginalCurrency(newValue);
		newSnapshot.setReferenceDate(today);

		// Set exchange rate
		String assetCurrency = asset.getOriginalCurrency().getValue();
		String userCurrency = asset.getUser().getCurrency().getValue();
		if (!assetCurrency.equals(userCurrency)) {
			BigDecimal rate = exchangeRateService.getRate(assetCurrency, userCurrency, today);
			newSnapshot.setExchangeRateToBase(rate);
		}

		snapshotRepository.save(newSnapshot);
		log.info(
			"Created automatic payment snapshot for liability {} (new value: {})",
			asset.getId(),
			newValue
		);

		if (newValue.compareTo(BigDecimal.ZERO) == 0) {
			asset.setIsActive(false);
			assetRepository.save(asset);
			log.info("Archived liability {} as it reached zero value", asset.getId());
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
