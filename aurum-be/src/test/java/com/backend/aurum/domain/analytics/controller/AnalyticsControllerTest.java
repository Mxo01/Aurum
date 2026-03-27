package com.backend.aurum.domain.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.AnalyticsSummaryDTO;
import com.backend.aurum.domain.analytics.dto.ChartDataDTO;
import com.backend.aurum.domain.analytics.facade.AnalyticsFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

	@Mock
	private AnalyticsFacade analyticsFacade;

	@InjectMocks
	private AnalyticsController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getSummary_returnsOkWithSummaryFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		AnalyticsSummaryDTO stubbedSummary = Instancio.create(AnalyticsSummaryDTO.class);
		when(analyticsFacade.getSummary(mockUserId)).thenReturn(stubbedSummary);

		// WHEN
		ResponseEntity<AnalyticsSummaryDTO> expectedResponse = testSubject.getSummary(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedSummary);
	}

	@Test
	void getChartData_withYear_delegatesToGetChartDataForYear() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYear = 2025;
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		ChartDataDTO stubbedChart = Instancio.create(ChartDataDTO.class);
		when(analyticsFacade.getChartDataForYear(mockUserId, mockYear)).thenReturn(stubbedChart);

		// WHEN
		ResponseEntity<ChartDataDTO> expectedResponse = testSubject.getChartData(
			mockYear,
			false,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedChart);
	}

	@Test
	void getChartData_withoutYear_delegatesToGetChartData() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		boolean mockAllHistory = true;
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		ChartDataDTO stubbedChart = Instancio.create(ChartDataDTO.class);
		when(analyticsFacade.getChartData(mockUserId, mockAllHistory)).thenReturn(stubbedChart);

		// WHEN
		ResponseEntity<ChartDataDTO> expectedResponse = testSubject.getChartData(
			null,
			mockAllHistory,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedChart);
	}

	@Test
	void getProjections_returnsOkWithProjectionsFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYears = 10;
		boolean mockAssetsOnly = false;
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		Map<Integer, BigDecimal> stubbedProjections = Map.of(1, BigDecimal.TEN, 5, BigDecimal.TEN);
		when(analyticsFacade.getProjections(mockUserId, mockYears, mockAssetsOnly)).thenReturn(
			stubbedProjections
		);

		// WHEN
		ResponseEntity<Map<Integer, BigDecimal>> expectedResponse = testSubject.getProjections(
			mockYears,
			mockAssetsOnly,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedProjections);
	}
}
