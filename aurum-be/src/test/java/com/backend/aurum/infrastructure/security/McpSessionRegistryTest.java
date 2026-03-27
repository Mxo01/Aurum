package com.backend.aurum.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.user.model.User;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class McpSessionRegistryTest {

	@InjectMocks
	private McpSessionRegistry testSubject;

	@Test
	void getUser_returnsUser_afterRegister() {
		// GIVEN
		String mockSessionId = UUID.randomUUID().toString();
		User mockUser = Instancio.create(User.class);

		// WHEN
		testSubject.register(mockSessionId, mockUser);
		Optional<User> expectedUser = testSubject.getUser(mockSessionId);

		// THEN
		assertThat(expectedUser).contains(mockUser);
	}

	@Test
	void getUser_returnsEmpty_whenSessionNotRegistered() {
		// GIVEN
		String mockSessionId = UUID.randomUUID().toString();

		// WHEN
		Optional<User> expectedUser = testSubject.getUser(mockSessionId);

		// THEN
		assertThat(expectedUser).isEmpty();
	}

	@Test
	void remove_makesSessionUnavailable() {
		// GIVEN
		String mockSessionId = UUID.randomUUID().toString();
		User mockUser = Instancio.create(User.class);
		testSubject.register(mockSessionId, mockUser);

		// WHEN
		testSubject.remove(mockSessionId);
		Optional<User> expectedUser = testSubject.getUser(mockSessionId);

		// THEN
		assertThat(expectedUser).isEmpty();
	}

	@Test
	void register_overwritesExistingSession_whenSameSessionIdUsed() {
		// GIVEN
		String mockSessionId = UUID.randomUUID().toString();
		User mockUser1 = Instancio.create(User.class);
		User mockUser2 = Instancio.create(User.class);
		testSubject.register(mockSessionId, mockUser1);

		// WHEN
		testSubject.register(mockSessionId, mockUser2);
		Optional<User> expectedUser = testSubject.getUser(mockSessionId);

		// THEN
		assertThat(expectedUser).contains(mockUser2);
	}

	@Test
	void evictExpiredSessions_doesNotRemoveActiveSessions() {
		// GIVEN
		String mockSessionId = UUID.randomUUID().toString();
		User mockUser = Instancio.create(User.class);
		testSubject.register(mockSessionId, mockUser);

		// WHEN
		testSubject.evictExpiredSessions();
		Optional<User> expectedUser = testSubject.getUser(mockSessionId);

		// THEN
		assertThat(expectedUser).contains(mockUser);
	}
}
