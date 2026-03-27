package com.backend.aurum.domain.mcp.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.mcp.dto.ApiKeyMetaDTO;
import com.backend.aurum.domain.mcp.dto.GeneratedKeyResponseDTO;
import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.service.ApiKeyService;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.util.Optional;
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
class ApiKeyControllerTest {

	@Mock
	private ApiKeyService apiKeyService;

	@InjectMocks
	private ApiKeyController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void generateKey_returnsOkWithGeneratedKey() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		String stubbedKey = "aurum_generatedkey123";
		when(apiKeyService.generateKey(mockPrincipal.user())).thenReturn(stubbedKey);

		// WHEN
		ResponseEntity<GeneratedKeyResponseDTO> expectedResponse = testSubject.generateKey(
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isNotNull();
		assertThat(expectedResponse.getBody().key()).isEqualTo(stubbedKey);
	}

	@Test
	void getKeyMeta_returnsOkWithMeta_whenKeyExists() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		ApiKey mockApiKey = Instancio.create(ApiKey.class);
		when(apiKeyService.getKeyMeta(mockUserId)).thenReturn(Optional.of(mockApiKey));

		// WHEN
		ResponseEntity<ApiKeyMetaDTO> expectedResponse = testSubject.getKeyMeta(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isNotNull();
	}

	@Test
	void getKeyMeta_returnsNotFound_whenNoKeyExists() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		when(apiKeyService.getKeyMeta(mockUserId)).thenReturn(Optional.empty());

		// WHEN
		ResponseEntity<ApiKeyMetaDTO> expectedResponse = testSubject.getKeyMeta(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void revokeKey_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.revokeKey(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(apiKeyService).revokeKey(mockUserId);
	}
}
