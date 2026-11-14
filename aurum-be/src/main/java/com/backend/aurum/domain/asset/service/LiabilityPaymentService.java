package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.currency.RateBook;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.user.enums.Currency;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pays one automatic liability forward from its persisted schedule.
 *
 * <p>One transaction per asset, owned here and not by the scheduler: a liability that fails to
 * resolve its rate rolls back its own payments and nothing else in the run.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LiabilityPaymentService {

	static final String SKIP_ALREADY_SETTLED = "already settled";
	static final String SKIP_NO_PAYMENT_FREQUENCY = "no payment frequency";
	static final String SKIP_NO_POSITIVE_PAYMENT_AMOUNT = "no positive payment amount";
	static final String SKIP_NO_VALUATION_SNAPSHOT = "no valuation snapshot";
	static final String SKIP_NO_OUTSTANDING_BALANCE = "no outstanding balance; marked settled";

	private static final int MONTHS_PER_YEAR = 12;

	private final AssetRepository assetRepository;
	private final SnapshotRepository snapshotRepository;
	private final SnapshotService snapshotService;
	private final CurrencyConversionService conversionService;
	private final Clock clock;

	@Transactional
	public LiabilityPaymentResult pay(UUID assetId, LocalDate today) {
		Asset asset = assetRepository
			.findById(Objects.requireNonNull(assetId))
			.orElseThrow(() -> {
				log.warn("LiabilityPaymentService#pay - Asset not found: assetId={}", assetId);
				return new NotFoundException("Asset not found");
			});

		if (asset.getSettledAt() != null) {
			return LiabilityPaymentResult.skipped(SKIP_ALREADY_SETTLED);
		}

		PaymentFrequency frequency = asset.getPaymentFrequency();
		if (frequency == null) {
			return LiabilityPaymentResult.skipped(SKIP_NO_PAYMENT_FREQUENCY);
		}

		BigDecimal paymentAmount = asset.getPaymentAmount();
		if (paymentAmount == null || paymentAmount.signum() <= 0) {
			return LiabilityPaymentResult.skipped(SKIP_NO_POSITIVE_PAYMENT_AMOUNT);
		}

		Snapshot latestSnapshot = snapshotRepository
			.findFirstByAssetIdOrderByReferenceDateDescIdAsc(asset.getId())
			.orElse(null);
		if (latestSnapshot == null) {
			return LiabilityPaymentResult.skipped(SKIP_NO_VALUATION_SNAPSHOT);
		}

		BigDecimal outstanding =
			latestSnapshot.getAmountOriginalCurrency() == null
				? BigDecimal.ZERO
				: latestSnapshot.getAmountOriginalCurrency();
		if (outstanding.signum() <= 0) {
			markSettled(asset);
			return LiabilityPaymentResult.skipped(SKIP_NO_OUTSTANDING_BALANCE);
		}

		int anchorDay = anchorDayOf(asset, latestSnapshot);
		boolean scheduleDerived = asset.getNextDueDate() == null || asset.getPaymentAnchorDay() == null;
		LocalDate due =
			asset.getNextDueDate() != null
				? asset.getNextDueDate()
				: advance(latestSnapshot.getReferenceDate(), anchorDay, frequency);
		asset.setPaymentAnchorDay((short) anchorDay);
		asset.setNextDueDate(due);
		if (due.isAfter(today)) {
			// The derived schedule is new state; an already persisted one must not cost an UPDATE.
			if (scheduleDerived) {
				assetRepository.save(asset);
			}
			return LiabilityPaymentResult.notDue(due);
		}

		Map<LocalDate, BigDecimal> balanceAfterPayment = new LinkedHashMap<>();
		BigDecimal balance = outstanding;
		while (!due.isAfter(today) && balance.signum() > 0) {
			balance = balance.subtract(paymentAmount).max(BigDecimal.ZERO);
			balanceAfterPayment.put(due, balance);
			due = advance(due, anchorDay, frequency);
		}

		insertScheduledSnapshots(asset, balanceAfterPayment);

		asset.setNextDueDate(due);
		if (balance.signum() == 0) {
			markSettled(asset);
		} else {
			assetRepository.save(asset);
		}
		return LiabilityPaymentResult.paid(balanceAfterPayment.size(), balance, asset.getNextDueDate());
	}

	/**
	 * One insert per due date and never an update: a scheduler row is a record of what happened on
	 * that date, and a user snapshot on the same date must survive it untouched.
	 */
	private void insertScheduledSnapshots(
		Asset asset,
		Map<LocalDate, BigDecimal> balanceAfterPayment
	) {
		RateBook rates = conversionService.book(
			userCurrencyOf(asset),
			List.of(asset.getOriginalCurrency()),
			balanceAfterPayment.keySet()
		);
		for (Map.Entry<LocalDate, BigDecimal> payment : balanceAfterPayment.entrySet()) {
			LocalDate dueDate = payment.getKey();
			snapshotService.saveScheduledPayment(
				asset,
				payment.getValue(),
				dueDate,
				rates.rate(asset.getOriginalCurrency(), dueDate)
			);
		}
	}

	private void markSettled(Asset asset) {
		asset.setSettledAt(LocalDateTime.now(clock));
		asset.setNextDueDate(null);
		asset.setPaymentAnchorDay(null);
		assetRepository.save(asset);
		log.info("Liability {} has no outstanding balance left and is now settled", asset.getId());
	}

	private Currency userCurrencyOf(Asset asset) {
		return asset.getUser() != null ? asset.getUser().getCurrency() : asset.getOriginalCurrency();
	}

	/**
	 * The anchor day is what keeps "the 30th of every month" stable across February: a bare date plus
	 * a month would drift to the 28th for good.
	 */
	private int anchorDayOf(Asset asset, Snapshot latestSnapshot) {
		Short persisted = asset.getPaymentAnchorDay();
		if (persisted != null) {
			return persisted.intValue();
		}
		LocalDate lastKnownDate =
			asset.getNextDueDate() != null ? asset.getNextDueDate() : latestSnapshot.getReferenceDate();
		return lastKnownDate.getDayOfMonth();
	}

	private LocalDate advance(LocalDate from, int anchorDay, PaymentFrequency frequency) {
		return switch (frequency) {
			case WEEKLY -> from.plusWeeks(1);
			case MONTHLY -> anchorMonth(from, anchorDay, 1);
			case YEARLY -> anchorMonth(from, anchorDay, MONTHS_PER_YEAR);
		};
	}

	private LocalDate anchorMonth(LocalDate from, int anchorDay, int months) {
		LocalDate target = from.plusMonths(months);
		return target.withDayOfMonth(Math.min(anchorDay, target.lengthOfMonth()));
	}
}
