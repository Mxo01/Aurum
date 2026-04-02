package com.backend.aurum.domain.asset.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.repository.AssetStatusLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AssetStatusLogRetentionServiceTest {

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 1);

	@Mock
	private AssetStatusLogRepository assetStatusLogRepository;

	private AssetStatusLogRetentionService testSubject;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
		testSubject = new AssetStatusLogRetentionService(assetStatusLogRepository, clock);
		ReflectionTestUtils.setField(testSubject, "retentionDays", 730);
	}

	@Test
	void purgeExpiredLogs_deletesLogsOlderThanRetentionPeriod() {
		// GIVEN
		LocalDate expectedCutoff = FIXED_TODAY.minusDays(730);
		when(assetStatusLogRepository.deleteByChangedAtBefore(expectedCutoff)).thenReturn(5);

		// WHEN
		testSubject.purgeExpiredLogs();

		// THEN
		verify(assetStatusLogRepository).deleteByChangedAtBefore(expectedCutoff);
	}

	@Test
	void purgeExpiredLogs_handlesZeroDeletedLogs() {
		// GIVEN
		LocalDate expectedCutoff = FIXED_TODAY.minusDays(730);
		when(assetStatusLogRepository.deleteByChangedAtBefore(expectedCutoff)).thenReturn(0);

		// WHEN
		testSubject.purgeExpiredLogs();

		// THEN
		verify(assetStatusLogRepository).deleteByChangedAtBefore(expectedCutoff);
	}
}
