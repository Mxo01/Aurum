package com.backend.aurum.infrastructure.security;

import com.backend.aurum.infrastructure.mcp.McpOriginValidationFilter;
import com.backend.aurum.infrastructure.mcp.McpSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String PROTECTED_RESOURCE_METADATA = "/.well-known/oauth-protected-resource";

	private static final String BARE_WILDCARD_ORIGIN = "*";

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	@Value("${cors.allowed-origins:http://localhost:4200}")
	private String corsAllowedOrigins;

	@Value("${mcp.allowed-origins:}")
	private String mcpAllowedOrigins;

	/**
	 * Serves the protected-resource metadata without installing the framework's
	 * default document (which has no authorization server and no scopes).
	 *
	 * <p>The filter is not added here because this chain has no resource-server
	 * configurer; the controller renders the canonical document.
	 */
	@Bean
	@Order(0)
	public SecurityFilterChain protectedResourceMetadataFilterChain(HttpSecurity http)
		throws Exception {
		http
			.securityMatcher(PROTECTED_RESOURCE_METADATA, PROTECTED_RESOURCE_METADATA + "/**")
			.csrf(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.requestCache(AbstractHttpConfigurer::disable)
			.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
		return http.build();
	}

	@Bean
	@Order(1)
	public SecurityFilterChain mcpFilterChain(
		HttpSecurity http,
		JwtConverter jwtConverter,
		McpBearerAuthenticationEntryPoint mcpEntryPoint,
		@Qualifier("mcpJwtDecoder") JwtDecoder mcpJwtDecoder
	) throws Exception {
		http
			.securityMatcher("/mcp")
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(mcpCorsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
			.addFilterBefore(new McpOriginValidationFilter(mcpAllowedOrigins), AuthorizationFilter.class)
			.oauth2ResourceServer(oauth2 ->
				oauth2
					.jwt(jwt -> jwt.decoder(mcpJwtDecoder).jwtAuthenticationConverter(jwtConverter))
					.authenticationEntryPoint(mcpEntryPoint)
			);
		return http.build();
	}

	@Bean
	@Order(2)
	public SecurityFilterChain filterChain(HttpSecurity http, JwtConverter jwtConverter)
		throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth ->
				auth
					.requestMatchers(
						"/v3/api-docs/**",
						"/swagger-ui/**",
						"/swagger-ui.html",
						"/actuator/health",
						"/error"
					)
					.permitAll()
					.anyRequest()
					.authenticated()
			)
			.oauth2ResourceServer(oauth2 ->
				oauth2.jwt(jwt -> jwt.decoder(jwtDecoder()).jwtAuthenticationConverter(jwtConverter))
			);

		return http.build();
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return buildJwtDecoder(audience);
	}

	/**
	 * The MCP resource only accepts tokens issued for its own audience.
	 *
	 * <p>This is the RFC 8707 requirement: a token minted for the web app must not
	 * be replayable against the MCP endpoint, even though both belong to the same
	 * user.
	 */
	@Bean
	public JwtDecoder mcpJwtDecoder(McpSettings settings) {
		return buildJwtDecoder(settings.audience());
	}

	private JwtDecoder buildJwtDecoder(String expectedAudience) {
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Collection<String>>(
			JwtClaimNames.AUD,
			aud -> aud != null && aud.contains(expectedAudience)
		);

		jwtDecoder.setJwtValidator(
			new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator)
		);

		return jwtDecoder;
	}

	private CorsConfigurationSource mcpCorsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = new ArrayList<>(Arrays.asList(mcpAllowedOrigins.split(",")));
		configuration.setAllowedOrigins(
			origins
				.stream()
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toList()
		);
		configuration.setAllowedMethods(List.of("GET", "POST", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(
			List.of("Authorization", "Content-Type", "Accept", "Mcp-Protocol-Version")
		);
		configuration.setExposedHeaders(List.of("WWW-Authenticate", "Mcp-Session-Id"));
		configuration.setAllowCredentials(false);
		configuration.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	/**
	 * The browser app's CORS policy.
	 *
	 * <p>The origin list is trimmed and validated once, before the source is handed to the
	 * filter chain. Credentials are allowed, and Spring combines a credentials-allowing
	 * configuration with an allowed origin pattern by reflecting the caller's
	 * {@code Origin}: the bare {@code *} entry would make every site an allowed origin, so
	 * startup fails instead of shipping that policy. Subdomain patterns such as
	 * {@code https://*.aurum-networth.com} stay allowed, because they still pin the host.
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		List<String> origins = parseCorsAllowedOrigins(corsAllowedOrigins);
		log.info("CORS allowed origins for the web API: {}", origins);

		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOriginPatterns(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}

	private static List<String> parseCorsAllowedOrigins(String raw) {
		List<String> origins = Arrays.stream(raw.split(",")).map(String::trim).toList();

		if (origins.contains(BARE_WILDCARD_ORIGIN)) {
			throw new IllegalStateException(
				"cors.allowed-origins must not be a bare wildcard when credentials are allowed"
			);
		}

		return List.copyOf(origins);
	}
}
