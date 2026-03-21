package com.backend.aurum.infrastructure.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

	@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String issuerUri;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	@Value("${cors.allowed-origins:http://localhost:4200}")
	private String corsAllowedOrigins;

	@Bean
	@Order(1)
	public SecurityFilterChain mcpFilterChain(HttpSecurity http, ApiKeyAuthFilter apiKeyAuthFilter)
		throws Exception {
		http
			.securityMatcher("/sse", "/mcp/message")
			.csrf(AbstractHttpConfigurer::disable)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
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
		NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(issuerUri).build();
		OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri);
		OAuth2TokenValidator<Jwt> audienceValidator = new JwtClaimValidator<Collection<String>>(
			JwtClaimNames.AUD,
			aud -> aud != null && aud.contains(audience)
		);

		jwtDecoder.setJwtValidator(
			new DelegatingOAuth2TokenValidator<>(issuerValidator, audienceValidator)
		);

		return jwtDecoder;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		List<String> origins = new ArrayList<>(Arrays.asList(corsAllowedOrigins.split(",")));
		origins.add("http://localhost:*");
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
