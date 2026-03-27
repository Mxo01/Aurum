package com.backend.aurum.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.service.UserService;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtConverterTest {

	@Mock
	private UserService userService;

	@InjectMocks
	private JwtConverter testSubject;

	@Test
	void convert_returnsAuthenticationWithUserPrincipal() {
		// GIVEN
		ReflectionTestUtils.setField(testSubject, "audience", "https://api.aurum-networth.com");
		String mockJwtId = "auth0|testuser";
		User mockUser = Instancio.of(User.class).set(Select.field(User::getJwtId), mockJwtId).create();
		Jwt mockJwt = mock(Jwt.class);
		when(mockJwt.getSubject()).thenReturn(mockJwtId);
		when(userService.findOrCreate(mockJwtId)).thenReturn(mockUser);

		// WHEN
		Authentication expectedAuth = testSubject.convert(mockJwt);

		// THEN
		assertThat(expectedAuth).isNotNull();
		assertThat(expectedAuth.getPrincipal()).isInstanceOf(UserPrincipal.class);
		UserPrincipal expectedPrincipal = (UserPrincipal) expectedAuth.getPrincipal();
		assertThat(expectedPrincipal.user()).isEqualTo(mockUser);
		assertThat(expectedPrincipal.jwt()).isEqualTo(mockJwt);
	}

	@Test
	void convert_callsFindOrCreateWithJwtSubject() {
		// GIVEN
		ReflectionTestUtils.setField(testSubject, "audience", "https://api.aurum-networth.com");
		String mockJwtId = "auth0|anothertestuser";
		User mockUser = Instancio.create(User.class);
		Jwt mockJwt = mock(Jwt.class);
		when(mockJwt.getSubject()).thenReturn(mockJwtId);
		when(userService.findOrCreate(mockJwtId)).thenReturn(mockUser);

		// WHEN
		testSubject.convert(mockJwt);

		// THEN — observable through returned principal's user being the one returned by findOrCreate
		Authentication expectedAuth = testSubject.convert(mockJwt);
		UserPrincipal expectedPrincipal = (UserPrincipal) expectedAuth.getPrincipal();
		assertThat(expectedPrincipal.user()).isEqualTo(mockUser);
	}
}
