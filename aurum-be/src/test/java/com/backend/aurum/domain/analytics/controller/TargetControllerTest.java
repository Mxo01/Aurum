package com.backend.aurum.domain.analytics.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.TargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.analytics.facade.TargetFacade;
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
class TargetControllerTest {

	@Mock
	private TargetFacade targetFacade;

	@InjectMocks
	private TargetController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getAllTargets_returnsOkWithDtosFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<TargetDTO> stubbedDtos = Instancio.ofList(TargetDTO.class).size(2).create();
		when(targetFacade.getAllTargets(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<TargetDTO>> expectedResponse = testSubject.getAllTargets(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void createTarget_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CreateTargetDTO mockDto = Instancio.create(CreateTargetDTO.class);
		TargetDTO stubbedDto = Instancio.create(TargetDTO.class);
		when(targetFacade.createTarget(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<TargetDTO> expectedResponse = testSubject.createTarget(mockDto, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void updateTarget_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		UpdateTargetDTO mockDto = Instancio.create(UpdateTargetDTO.class);
		TargetDTO stubbedDto = Instancio.create(TargetDTO.class);
		when(targetFacade.updateTarget(mockId, mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<TargetDTO> expectedResponse = testSubject.updateTarget(
			mockId,
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void deleteTarget_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteTarget(mockId, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(targetFacade).deleteTarget(mockId, mockUserId);
	}
}
