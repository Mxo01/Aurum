package com.backend.aurum.domain.user.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.dto.UserExportDTO;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

	@Mock
	private UserService userService;

	@InjectMocks
	private UserController testSubject;

	private UserPrincipal buildPrincipal(UUID userId, String jwtId) {
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), userId)
			.set(Select.field(User::getJwtId), jwtId)
			.create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getUser_returnsOkWithUserFromService() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "auth0|user");
		User stubbedUser = Instancio.create(User.class);
		when(userService.getUserById(mockUserId)).thenReturn(stubbedUser);

		// WHEN
		ResponseEntity<User> expectedResponse = testSubject.getUser(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedUser);
	}

	@Test
	void exportUserData_returnsOkWithExportFromService() {
		// GIVEN
		ReflectionTestUtils.setField(testSubject, "audience", "https://api.aurum-networth.com");
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|user";
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, mockJwtId);
		String mockEmail = "test@test.com";
		UserExportDTO stubbedExport = new UserExportDTO(
			new UserExportDTO.ProfileExport(mockUserId, mockEmail, "EUR", "EN_US"),
			List.of(),
			List.of(),
			List.of()
		);
		when(mockPrincipal.jwt().getClaimAsString("https://api.aurum-networth.com/email")).thenReturn(
			mockEmail
		);
		when(userService.exportUserData(mockUserId, mockEmail)).thenReturn(stubbedExport);

		// WHEN
		ResponseEntity<UserExportDTO> expectedResponse = testSubject.exportUserData(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedExport);
	}

	@Test
	void deleteUser_delegatesToServiceAndReturnsOk() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|deleteuser";
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, mockJwtId);
		when(mockPrincipal.jwt().getSubject()).thenReturn(mockJwtId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteUser(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).deleteUser(mockUserId, mockJwtId);
	}

	@Test
	void updateName_delegatesToServiceAndReturnsOk() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|updatename";
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, mockJwtId);
		when(mockPrincipal.jwt().getSubject()).thenReturn(mockJwtId);
		UpdateNameDTO mockDto = new UpdateNameDTO("New Name");

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.updateName(mockPrincipal, mockDto);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).updateAuth0Name(mockJwtId, mockDto);
	}

	@Test
	void updateCurrency_delegatesToServiceAndReturnsOk() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "auth0|currency");
		UpdateCurrencyDTO mockDto = new UpdateCurrencyDTO(Currency.USD);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.updateCurrency(mockPrincipal, mockDto);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).updateCurrency(mockUserId, mockDto);
	}

	@Test
	void updateLocale_delegatesToServiceAndReturnsOk() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "auth0|locale");
		UpdateLocaleDTO mockDto = new UpdateLocaleDTO(Locale.EN_US);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.updateLocale(mockPrincipal, mockDto);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).updateLocale(mockUserId, mockDto);
	}

	@Test
	void updatePicture_returnsForbidden_whenGoogleUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "google-oauth2|user");
		MockMultipartFile mockFile = new MockMultipartFile("file", new byte[0]);
		when(userService.isGoogleUser(mockPrincipal)).thenReturn(true);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.updatePicture(mockPrincipal, mockFile);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void updatePicture_delegatesToService_whenNotGoogleUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "auth0|user");
		MockMultipartFile mockFile = new MockMultipartFile("file", new byte[0]);
		when(userService.isGoogleUser(mockPrincipal)).thenReturn(false);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.updatePicture(mockPrincipal, mockFile);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).updatePicture(mockUserId, mockFile);
	}

	@Test
	void deletePicture_returnsForbidden_whenGoogleUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "google-oauth2|user");
		when(userService.isGoogleUser(mockPrincipal)).thenReturn(true);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deletePicture(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void deletePicture_delegatesToService_whenNotGoogleUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId, "auth0|user");
		when(userService.isGoogleUser(mockPrincipal)).thenReturn(false);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deletePicture(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		verify(userService).deletePicture(mockUserId);
	}
}
