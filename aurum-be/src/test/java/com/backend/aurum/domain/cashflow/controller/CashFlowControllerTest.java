package com.backend.aurum.domain.cashflow.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.cashflow.dto.CashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.dto.CashFlowYearDTO;
import com.backend.aurum.domain.cashflow.dto.UpdateCashFlowEntryDTO;
import com.backend.aurum.domain.cashflow.facade.CashFlowFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.util.List;
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
class CashFlowControllerTest {

	@Mock
	private CashFlowFacade cashFlowFacade;

	@InjectMocks
	private CashFlowController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getYear_returnsOkWithYearData() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYear = 2025;
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CashFlowYearDTO stubbedYear = CashFlowYearDTO.builder()
			.year(mockYear)
			.entries(List.of())
			.previousYearEntries(List.of())
			.availableYears(List.of(mockYear))
			.build();
		when(cashFlowFacade.getYear(mockUserId, mockYear)).thenReturn(stubbedYear);

		// WHEN
		ResponseEntity<CashFlowYearDTO> expectedResponse = testSubject.getYear(mockYear, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedYear);
	}

	@Test
	void updateEntry_returnsOkWithUpdatedEntry() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		int mockYear = 2025;
		int mockMonth = 3;
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		UpdateCashFlowEntryDTO mockDto = Instancio.create(UpdateCashFlowEntryDTO.class);
		CashFlowEntryDTO stubbedEntry = Instancio.create(CashFlowEntryDTO.class);
		when(cashFlowFacade.updateEntry(mockUserId, mockYear, mockMonth, mockDto)).thenReturn(
			stubbedEntry
		);

		// WHEN
		ResponseEntity<CashFlowEntryDTO> expectedResponse = testSubject.updateEntry(
			mockYear,
			mockMonth,
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedEntry);
		verify(cashFlowFacade).updateEntry(mockUserId, mockYear, mockMonth, mockDto);
	}
}
