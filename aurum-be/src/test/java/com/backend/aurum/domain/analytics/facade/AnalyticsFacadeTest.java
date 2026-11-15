package com.backend.aurum.domain.analytics.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.service.AnalyticsService;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsFacadeTest {

	private static final int MINIMUM_PROJECTION_YEARS = 1;

	private static final int MAXIMUM_PROJECTION_YEARS = 50;

	private static final int YEAR_COUNT_BELOW_THE_BOUND = 0;

	private static final int NEGATIVE_YEAR_COUNT = -1;

	private static final int YEAR_COUNT_ABOVE_THE_BOUND = 51;

	private static final String YEARS_FIELD = "years";

	private static final String YEARS_OUT_OF_RANGE_MESSAGE = "Years must be between 1 and 50";

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

	@ParameterizedTest(name = "years {0}")
	@MethodSource("rejectedProjectionYears")
	void getProjections_rejectsAYearCountOutsideTheBound(int years) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		boolean mockAssetsOnly = false;

		// WHEN
		ValidationException expectedViolation = catchThrowableOfType(
			() -> testSubject.getProjections(mockUserId, years, mockAssetsOnly),
			ValidationException.class
		);

		// THEN
		assertThat(expectedViolation.getField()).isEqualTo(YEARS_FIELD);
		assertThat(expectedViolation).hasMessage(YEARS_OUT_OF_RANGE_MESSAGE);
		verifyNoInteractions(analyticsService);
	}

	static IntStream rejectedProjectionYears() {
		return IntStream.of(
			YEAR_COUNT_BELOW_THE_BOUND,
			NEGATIVE_YEAR_COUNT,
			YEAR_COUNT_ABOVE_THE_BOUND
		);
	}

	@ParameterizedTest(name = "years {0}")
	@MethodSource("acceptedProjectionYears")
	void getProjections_passesAYearCountAtTheBoundToTheService(int years) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		boolean mockAssetsOnly = true;
		Map<Integer, BigDecimal> stubbedProjections = Map.of(2025, BigDecimal.TEN);
		when(analyticsService.getProjections(mockUserId, years, mockAssetsOnly)).thenReturn(
			stubbedProjections
		);

		// WHEN
		Map<Integer, BigDecimal> expectedProjections = testSubject.getProjections(
			mockUserId,
			years,
			mockAssetsOnly
		);

		// THEN
		assertThat(expectedProjections).isEqualTo(stubbedProjections);
	}

	static Stream<Integer> acceptedProjectionYears() {
		return Stream.of(MINIMUM_PROJECTION_YEARS, MAXIMUM_PROJECTION_YEARS);
	}
}
