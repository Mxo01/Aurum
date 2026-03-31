package com.backend.aurum.domain.cashflow.service;

import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.repository.CashFlowRepository;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
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
public class CashFlowService {

	private final CashFlowRepository cashFlowRepository;
	private final UserRepository userRepository;

	@Transactional(readOnly = true)
	public List<CashFlow> getEntriesForYear(UUID userId, Integer year) {
		log.debug(
			"CashFlowService#getEntriesForYear - Fetching cash flow entries for userId={}, year={}",
			userId,
			year
		);
		return cashFlowRepository.findByUserIdAndYearOrderByMonthAsc(userId, year);
	}

	@Transactional(readOnly = true)
	public List<Integer> getAvailableYears(UUID userId) {
		log.debug("CashFlowService#getAvailableYears - Fetching available years for userId={}", userId);
		return cashFlowRepository.findDistinctYearsByUserId(userId);
	}

	@Transactional
	public CashFlow upsertEntry(
		UUID userId,
		Integer year,
		Integer month,
		BigDecimal earned,
		BigDecimal spent
	) {
		log.debug(
			"CashFlowService#upsertEntry - Upserting cash flow for userId={}, year={}, month={}",
			userId,
			year,
			month
		);

		CashFlow entry = cashFlowRepository
			.findByUserIdAndYearAndMonth(userId, year, month)
			.orElseGet(() -> {
				User user = userRepository
					.findById(Objects.requireNonNull(userId))
					.orElseThrow(() -> new RuntimeException("User not found"));
				return CashFlow.builder().user(user).year(year).month(month).build();
			});

		if (earned != null) entry.setEarned(earned);
		if (spent != null) entry.setSpent(spent);

		CashFlow saved = cashFlowRepository.save(Objects.requireNonNull(entry));
		log.info(
			"CashFlowService#upsertEntry - Cash flow entry saved: id={}, year={}, month={}",
			saved.getId(),
			year,
			month
		);
		return saved;
	}
}
