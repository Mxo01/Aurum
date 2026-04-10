package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class McpAuthorizationServerMetadataControllerTest {

	@InjectMocks
	private McpAuthorizationServerMetadataController testSubject;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(testSubject, "resourceUrl", "https://api.test.com");
		ReflectionTestUtils.setField(testSubject, "issuerUri", "https://auth.test.com/");
	}

	@Test
	void authorizationServerMetadata_setsIssuerToResourceUrl() {
		// WHEN
		Map<String, Object> result = testSubject.authorizationServerMetadata();

		// THEN
		assertThat(result.get("issuer")).isEqualTo("https://api.test.com");
	}

	@Test
	void authorizationServerMetadata_proxiesAuthorizationEndpointThroughBackend() {
		// WHEN
		Map<String, Object> result = testSubject.authorizationServerMetadata();

		// THEN
		assertThat(result.get("authorization_endpoint")).isEqualTo(
			"https://api.test.com/oauth/authorize"
		);
	}

	@Test
	void authorizationServerMetadata_pointsTokenEndpointToAuth0() {
		// WHEN
		Map<String, Object> result = testSubject.authorizationServerMetadata();

		// THEN
		assertThat(result.get("token_endpoint")).isEqualTo("https://auth.test.com/oauth/token");
	}

	@Test
	void authorizationServerMetadata_exposesRegistrationEndpointOnBackend() {
		// WHEN
		Map<String, Object> result = testSubject.authorizationServerMetadata();

		// THEN
		assertThat(result.get("registration_endpoint")).isEqualTo(
			"https://api.test.com/oauth/register"
		);
	}

	@Test
	void authorizationServerMetadata_supportsPkce() {
		// WHEN
		Map<String, Object> result = testSubject.authorizationServerMetadata();

		// THEN
		assertThat(result.get("code_challenge_methods_supported")).isEqualTo(List.of("S256"));
	}
}
