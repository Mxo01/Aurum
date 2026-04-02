package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssetStatusLogRetentionService {

	private final AssetStatusLogRepository assetStatusLogRepository;
	private final Clock clock;

	@Value("${aurum.retention.asset-status-log-days:730}")
	private int retentionDays;

	@Scheduled(cron = "0 0 2 * * *", zone = "UTC")
	@Transactional
	public void purgeExpiredLogs() {
		LocalDate cutoff = LocalDate.now(clock).minusDays(retentionDays);
		log.info(
			"AssetStatusLogRetentionService#purgeExpiredLogs - Purging logs older than {} (retention: {} days)",
			cutoff,
			retentionDays
		);
		int deleted = assetStatusLogRepository.deleteByChangedAtBefore(cutoff);
		log.info(
			"AssetStatusLogRetentionService#purgeExpiredLogs - Deleted {} expired log(s)",
			deleted
		);
	}
}
