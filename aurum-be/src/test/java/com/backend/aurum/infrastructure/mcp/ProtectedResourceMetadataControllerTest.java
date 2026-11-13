package com.backend.aurum.infrastructure.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProtectedResourceMetadataControllerTest {

	@Mock
	private McpSettings settings;

	@Mock
	private McpScopes scopes;

	@InjectMocks
	private ProtectedResourceMetadataController testSubject;

	@Test
	void protectedResourceMetadata_returnsResourceIssuerAndBearerMethod() {
		// GIVEN
		when(settings.resourceUrl()).thenReturn("https://api.test.com/mcp");
		when(settings.issuer()).thenReturn("https://auth.test.com/");
		when(scopes.all()).thenReturn(List.of());

		// WHEN
		Map<String, Object> result = testSubject.protectedResourceMetadata();

		// THEN
		assertThat(result).containsEntry("resource", "https://api.test.com/mcp");
		assertThat(result).containsEntry("authorization_servers", List.of("https://auth.test.com/"));
		assertThat(result).containsEntry("bearer_methods_supported", List.of("header"));
		assertThat(result).doesNotContainKey("scopes_supported");
	}

	@Test
	void protectedResourceMetadata_advertisesConfiguredScopes() {
		// GIVEN
		when(settings.resourceUrl()).thenReturn("https://api.test.com/mcp");
		when(settings.issuer()).thenReturn("https://auth.test.com/");
		when(scopes.all()).thenReturn(List.of("mcp:read", "mcp:write"));

		// WHEN
		Map<String, Object> result = testSubject.protectedResourceMetadata();

		// THEN
		assertThat(result).containsEntry("scopes_supported", List.of("mcp:read", "mcp:write"));
	}
}
