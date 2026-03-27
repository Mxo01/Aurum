package com.backend.aurum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class Auth0ManagementServiceTest {

	@InjectMocks
	private Auth0ManagementService testSubject;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(testSubject, "domain", "test.auth0.com");
		ReflectionTestUtils.setField(testSubject, "clientId", "testClientId");
		ReflectionTestUtils.setField(testSubject, "clientSecret", "testClientSecret");
	}

	@Test
	void getAccessToken_returnsCachedToken_whenTokenIsStillValid() throws Exception {
		// GIVEN
		String stubbedToken = "cached_token_value";
		ReflectionTestUtils.setField(testSubject, "cachedToken", stubbedToken);
		ReflectionTestUtils.setField(testSubject, "expiresAt", Instant.now().plusSeconds(300));

		// WHEN
		String expectedToken = testSubject.getAccessToken();

		// THEN
		assertThat(expectedToken).isEqualTo(stubbedToken);
	}
}
