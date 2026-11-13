package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	private static final JwtGrantedAuthoritiesConverter AUTHORITIES =
		new JwtGrantedAuthoritiesConverter();

	private final UserService userService;

	@Override
	public AbstractAuthenticationToken convert(@NonNull Jwt jwt) {
		String jwtId = jwt.getSubject();
		if (jwtId == null || jwtId.isBlank() || jwtId.endsWith("@clients")) {
			throw new BadCredentialsException("A user-delegated token is required");
		}

		User user = userService.findOrCreate(jwtId);

		UserPrincipal principal = new UserPrincipal(user, jwt);
		Collection<GrantedAuthority> authorities = AUTHORITIES.convert(jwt);

		return new JwtAuthenticationToken(jwt, authorities, jwtId) {
			@Override
			public Object getPrincipal() {
				return principal;
			}
		};
	}
}
