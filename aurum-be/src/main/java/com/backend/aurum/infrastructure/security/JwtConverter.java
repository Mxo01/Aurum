package com.backend.aurum.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.backend.aurum.domain.user.dto.UserPrincipal;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.service.UserService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {
	private final UserService userService;

	@Value("${spring.security.oauth2.resourceserver.jwt.audiences}")
	private String audience;

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		String jwtId = jwt.getSubject();
		String email = jwt.getClaimAsString(audience + "/email");

		System.out.println("JWT ID: " + jwtId);
		System.out.println("Email: " + email);

		User user = userService.findOrCreate(jwtId, email);

		UserPrincipal principal = new UserPrincipal(user, jwt);

		return new JwtAuthenticationToken(jwt, null, jwtId) {
			@Override
			public Object getPrincipal() {
				return principal;
			}
		};
	}
}