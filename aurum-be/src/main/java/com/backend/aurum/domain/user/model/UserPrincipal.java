package com.backend.aurum.domain.user.model;

import org.springframework.security.oauth2.jwt.Jwt;

public record UserPrincipal(
		User user,
		Jwt jwt) {
}
