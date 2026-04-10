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
class ProtectedResourceMetadataControllerTest {

	@InjectMocks
	private ProtectedResourceMetadataController testSubject;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(testSubject, "resourceUrl", "https://api.test.com");
		ReflectionTestUtils.setField(testSubject, "issuerUri", "https://auth.test.com/");
	}

	@Test
	void protectedResourceMetadata_returnsCorrectStructure() {
		// WHEN
		Map<String, Object> result = testSubject.protectedResourceMetadata();

		// THEN
		assertThat(result.get("resource")).isEqualTo("https://api.test.com");
		assertThat(result.get("authorization_servers")).isEqualTo(List.of("https://api.test.com"));
		assertThat(result.get("scopes_supported")).isEqualTo(List.of("openid", "profile"));
		assertThat(result.get("bearer_methods_supported")).isEqualTo(List.of("header"));
	}

	@Test
	void protectedResourceMetadata_containsAllRequiredFields() {
		// WHEN
		Map<String, Object> result = testSubject.protectedResourceMetadata();

		// THEN
		assertThat(result).containsKeys(
			"resource",
			"authorization_servers",
			"scopes_supported",
			"bearer_methods_supported"
		);
	}
}
