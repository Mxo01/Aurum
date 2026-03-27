package com.backend.aurum.domain.analytics.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsFacadeTest {

	@Mock
	private AnalyticsService analyticsService;

	@InjectMocks
	private AnalyticsFacade testSubject;

	@Test
	void getSummary_delegatesToAnalyticsService() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AnalyticsSummaryDTO stubbedSummary = Instancio.create(AnalyticsSummaryDTO.class);
		when(analyticsService.getSummary(mockUserId)).thenReturn(stubbedSummary);

		// WHEN
		AnalyticsSummaryDTO expectedSummary = testSubject.getSummary(mockUserId);

		// THEN
		assertThat(expectedSummary).isEqualTo(stubbedSummary);
	}

	@Test
	void getChartData_delegatesToAnalyticsService() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		boolean mockAllHistory = true;
		ChartDataDTO stubbedChartData = Instancio.create(ChartDataDTO.class);
		when(analyticsService.getChartData(mockUserId, mockAllHistory)).thenReturn(stubbedChartData);

		// WHEN
		ChartDataDTO expectedChartData = testSubject.getChartData(mockUserId, mockAllHistory);

		// THEN
		assertThat(expectedChartData).isEqualTo(stubbedChartData);
	}

	@Test
	void getChartDataForYear_delegatesToAnalyticsService() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYear = 2025;
		ChartDataDTO stubbedChartData = Instancio.create(ChartDataDTO.class);
		when(analyticsService.getChartDataForYear(mockUserId, mockYear)).thenReturn(stubbedChartData);

		// WHEN
		ChartDataDTO expectedChartData = testSubject.getChartDataForYear(mockUserId, mockYear);

		// THEN
		assertThat(expectedChartData).isEqualTo(stubbedChartData);
	}

	@Test
	void getProjections_delegatesToAnalyticsService() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYears = 5;
		boolean mockAssetsOnly = false;
		Map<Integer, BigDecimal> stubbedProjections = Map.of(
			2025,
			BigDecimal.TEN,
			2026,
			BigDecimal.ONE
		);
		when(analyticsService.getProjections(mockUserId, mockYears, mockAssetsOnly)).thenReturn(
			stubbedProjections
		);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(
			mockUserId,
			mockYears,
			mockAssetsOnly
		);

		// THEN
		assertThat(expectedProjections).isEqualTo(stubbedProjections);
	}
}
