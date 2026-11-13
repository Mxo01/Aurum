package com.backend.aurum.infrastructure.security;

import com.backend.aurum.infrastructure.mcp.McpOriginValidationFilter;
import com.backend.aurum.infrastructure.mcp.McpSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	private static final String PROTECTED_RESOURCE_METADATA = "/.well-known/oauth-protected-resource";

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	@Value("${cors.allowed-origins:http://localhost:4200}")
	private String corsAllowedOrigins;

	@Value("${mcp.allowed-origins:}")
	private String mcpAllowedOrigins;

	@Bean
	@Order(1)
	public SecurityFilterChain mcpFilterChain(
		HttpSecurity http,
		JwtConverter jwtConverter,
		McpBearerAuthenticationEntryPoint mcpEntryPoint,
		@Qualifier("mcpJwtDecoder") JwtDecoder mcpJwtDecoder
	) throws Exception {
		http
			.securityMatcher("/mcp", PROTECTED_RESOURCE_METADATA, PROTECTED_RESOURCE_METADATA + "/mcp")
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(mcpCorsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.authorizeHttpRequests(auth ->
				auth
					.requestMatchers(PROTECTED_RESOURCE_METADATA, PROTECTED_RESOURCE_METADATA + "/mcp")
					.permitAll()
					.anyRequest()
					.authenticated()
			)
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
					.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/error")
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

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = new ArrayList<>(Arrays.asList(corsAllowedOrigins.split(",")));
		configuration.setAllowedOriginPatterns(origins);
		configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
		configuration.setExposedHeaders(List.of("Authorization"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);

		return source;
	}
}
