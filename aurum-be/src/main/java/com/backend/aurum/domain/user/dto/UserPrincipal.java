package com.backend.aurum.domain.user.dto;

import org.springframework.security.oauth2.jwt.Jwt;

import com.backend.aurum.domain.user.model.User;

public record UserPrincipal(
		User user,
		Jwt jwt) {
}
