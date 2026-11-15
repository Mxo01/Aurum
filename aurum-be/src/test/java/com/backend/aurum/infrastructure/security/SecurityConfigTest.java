package com.backend.aurum.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class SecurityConfigTest {

	private static final String CORS_ALLOWED_ORIGINS_FIELD = "corsAllowedOrigins";

	private static final String CORS_PROPERTY = "cors.allowed-origins";

	private static final String RULE_FRAGMENT = "bare wildcard";

	private static final String CREDENTIALS_FRAGMENT = "credentials";

	private static final String WEB_ORIGIN = "https://aurum-networth.com";

	private static final String SECOND_WEB_ORIGIN = "http://localhost:4200";

	private static final String SUBDOMAIN_WILDCARD_ORIGIN = "https://*.aurum-networth.com";

	private static final String BARE_WILDCARD_ORIGIN = "*";

	private static final String PADDED_BARE_WILDCARD_ORIGIN = " " + BARE_WILDCARD_ORIGIN + " ";

	private static final String ANY_REQUEST_PATH = "/assets";

	private static final String REQUEST_METHOD = "GET";

	private static final List<String> EXPECTED_METHODS = List.of(
		"GET",
		"POST",
		"PUT",
		"PATCH",
		"DELETE",
		"OPTIONS"
	);

	private static final List<String> EXPECTED_HEADERS = List.of(
		"Authorization",
		"Content-Type",
		"Accept"
	);

	private static final List<String> EXPECTED_EXPOSED_HEADERS = List.of("Authorization");

	@ParameterizedTest(name = "{0}")
	@MethodSource("bareWildcardCaseProvider")
	void corsConfigurationSource_failsStartup_whenABareWildcardEntryIsConfigured(
		String scenario,
		String corsAllowedOrigins
	) {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(corsAllowedOrigins);

		// WHEN
		Throwable actualFailure = thrownByCorsConfigurationSourceOf(testSubject);

		// THEN
		assertThat(actualFailure)
			.isInstanceOf(IllegalStateException.class)
			.hasMessageContaining(CORS_PROPERTY)
			.hasMessageContaining(RULE_FRAGMENT)
			.hasMessageContaining(CREDENTIALS_FRAGMENT);
	}

	static Stream<Arguments> bareWildcardCaseProvider() {
		return Stream.of(
			Arguments.of("a bare wildcard entry alone", BARE_WILDCARD_ORIGIN),
			Arguments.of(
				"a bare wildcard entry among explicit origins",
				WEB_ORIGIN + "," + BARE_WILDCARD_ORIGIN
			),
			Arguments.of("a whitespace-padded bare wildcard entry", PADDED_BARE_WILDCARD_ORIGIN)
		);
	}

	@Test
	void corsConfigurationSource_acceptsAnExplicitOriginList() {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(WEB_ORIGIN + "," + SECOND_WEB_ORIGIN);

		// WHEN
		CorsConfiguration expectedConfiguration = corsConfigurationOf(testSubject);

		// THEN
		assertThat(expectedConfiguration.getAllowedOriginPatterns()).containsExactly(
			WEB_ORIGIN,
			SECOND_WEB_ORIGIN
		);
	}

	@Test
	void corsConfigurationSource_trimsEveryConfiguredOrigin() {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(
			" " + WEB_ORIGIN + " , " + SECOND_WEB_ORIGIN + " "
		);

		// WHEN
		CorsConfiguration expectedConfiguration = corsConfigurationOf(testSubject);

		// THEN
		assertThat(expectedConfiguration.getAllowedOriginPatterns()).containsExactly(
			WEB_ORIGIN,
			SECOND_WEB_ORIGIN
		);
	}

	@Test
	void corsConfigurationSource_acceptsASubdomainWildcardOriginPattern() {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(SUBDOMAIN_WILDCARD_ORIGIN);

		// WHEN
		CorsConfiguration expectedConfiguration = corsConfigurationOf(testSubject);

		// THEN
		assertThat(expectedConfiguration.getAllowedOriginPatterns()).containsExactly(
			SUBDOMAIN_WILDCARD_ORIGIN
		);
	}

	@Test
	void corsConfigurationSource_keepsTheWebApiPolicy_whenTheOriginListIsExplicit() {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(WEB_ORIGIN);

		// WHEN
		CorsConfiguration expectedConfiguration = corsConfigurationOf(testSubject);

		// THEN
		assertThat(expectedConfiguration.getAllowedMethods()).containsExactlyElementsOf(
			EXPECTED_METHODS
		);
		assertThat(expectedConfiguration.getAllowedHeaders()).containsExactlyElementsOf(
			EXPECTED_HEADERS
		);
		assertThat(expectedConfiguration.getExposedHeaders()).containsExactlyElementsOf(
			EXPECTED_EXPOSED_HEADERS
		);
		assertThat(expectedConfiguration.getAllowCredentials()).isTrue();
	}

	@Test
	void corsConfigurationSource_logsTheTrimmedEffectiveOriginList() {
		// GIVEN
		SecurityConfig testSubject = buildSecurityConfig(
			" " + WEB_ORIGIN + " , " + SECOND_WEB_ORIGIN + " "
		);
		Logger mockSecurityConfigLogger = (Logger) LoggerFactory.getLogger(SecurityConfig.class);
		ListAppender<ILoggingEvent> mockAppender = new ListAppender<>();
		mockAppender.start();
		mockSecurityConfigLogger.addAppender(mockAppender);

		try {
			// WHEN
			testSubject.corsConfigurationSource();

			// THEN
			assertThat(mockAppender.list).hasSize(1);
			ILoggingEvent expectedLogEvent = mockAppender.list.getFirst();
			assertThat(expectedLogEvent.getLevel()).isEqualTo(Level.INFO);
			assertThat(expectedLogEvent.getFormattedMessage()).contains(
				"[" + WEB_ORIGIN + ", " + SECOND_WEB_ORIGIN + "]"
			);
		} finally {
			mockSecurityConfigLogger.detachAppender(mockAppender);
			mockAppender.stop();
		}
	}

	private static SecurityConfig buildSecurityConfig(String corsAllowedOrigins) {
		SecurityConfig mockSecurityConfig = new SecurityConfig();
		ReflectionTestUtils.setField(
			mockSecurityConfig,
			CORS_ALLOWED_ORIGINS_FIELD,
			corsAllowedOrigins
		);
		return mockSecurityConfig;
	}

	private static Throwable thrownByCorsConfigurationSourceOf(SecurityConfig securityConfig) {
		return catchThrowable(securityConfig::corsConfigurationSource);
	}

	private static CorsConfiguration corsConfigurationOf(SecurityConfig securityConfig) {
		CorsConfigurationSource expectedSource = securityConfig.corsConfigurationSource();
		CorsConfiguration expectedConfiguration = expectedSource.getCorsConfiguration(
			new MockHttpServletRequest(REQUEST_METHOD, ANY_REQUEST_PATH)
		);
		assertThat(expectedConfiguration).isNotNull();
		return expectedConfiguration;
	}
}
