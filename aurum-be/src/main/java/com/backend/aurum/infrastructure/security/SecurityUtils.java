package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

	private final UserService userService;

	public UUID getCurrentUserId() {
		Jwt jwt = getCurrentJwt();
		String jwtId = jwt.getSubject();
		String email = jwt.getClaimAsString("email");
		User user = userService.findOrCreate(jwtId, email);

		return user.getId();
	}

	private Jwt getCurrentJwt() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();

		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			return jwtAuth.getToken();
		}

		throw new IllegalStateException(
				"No JWT authentication found in security context. " +
						"Expected JwtAuthenticationToken, got: " +
						(auth == null ? "null" : auth.getClass().getName()));
	}
}
