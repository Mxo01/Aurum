package com.backend.aurum.domain.mcp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.repository.ApiKeyRepository;
import com.backend.aurum.domain.user.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ApiKeyServiceTest {

	@Mock
	private ApiKeyRepository apiKeyRepository;

	@InjectMocks
	private ApiKeyService testSubject;

	@Test
	void generateKey_returnsPlainKeyWithPrefix() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		when(apiKeyRepository.save(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			Instancio.create(ApiKey.class)
		);

		// WHEN
		String expectedKey = testSubject.generateKey(mockUser);

		// THEN
		assertThat(expectedKey).startsWith("aurum_");
	}

	@Test
	void generateKey_revokesExistingKeyBeforeSaving() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		when(apiKeyRepository.save(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			Instancio.create(ApiKey.class)
		);

		// WHEN
		testSubject.generateKey(mockUser);

		// THEN
		verify(apiKeyRepository).deleteByUserId(mockUser.getId());
	}

	@Test
	void resolveUser_returnsEmpty_whenKeyNotInDb() {
		// GIVEN
		String mockPlainKey = "aurum_unknownkey123";
		when(apiKeyRepository.findByKeyHash(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			Optional.empty()
		);

		// WHEN
		Optional<User> expectedUser = testSubject.resolveUser(mockPlainKey);

		// THEN
		assertThat(expectedUser).isEmpty();
	}

	@Test
	void resolveUser_returnsUser_whenKeyFoundInDb() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		ApiKey mockApiKey = Instancio.of(ApiKey.class)
			.set(Select.field(ApiKey::getUser), mockUser)
			.create();
		String mockPlainKey = "aurum_validkey123";
		when(apiKeyRepository.findByKeyHash(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			Optional.of(mockApiKey)
		);

		// WHEN
		Optional<User> expectedUser = testSubject.resolveUser(mockPlainKey);

		// THEN
		assertThat(expectedUser).contains(mockUser);
	}

	@Test
	void resolveUser_returnsCachedUser_onSecondCall() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		ApiKey mockApiKey = Instancio.of(ApiKey.class)
			.set(Select.field(ApiKey::getUser), mockUser)
			.create();
		String mockPlainKey = "aurum_cachedkey123";
		when(apiKeyRepository.findByKeyHash(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			Optional.of(mockApiKey)
		);

		// WHEN
		testSubject.resolveUser(mockPlainKey);
		Optional<User> expectedUser = testSubject.resolveUser(mockPlainKey);

		// THEN
		assertThat(expectedUser).contains(mockUser);
	}

	@Test
	void getKeyMeta_returnsFirstKey_whenKeyExists() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		ApiKey mockApiKey = Instancio.create(ApiKey.class);
		when(apiKeyRepository.findByUserId(mockUserId)).thenReturn(List.of(mockApiKey));

		// WHEN
		Optional<ApiKey> expectedKey = testSubject.getKeyMeta(mockUserId);

		// THEN
		assertThat(expectedKey).contains(mockApiKey);
	}

	@Test
	void getKeyMeta_returnsEmpty_whenNoKeyExists() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(apiKeyRepository.findByUserId(mockUserId)).thenReturn(List.of());

		// WHEN
		Optional<ApiKey> expectedKey = testSubject.getKeyMeta(mockUserId);

		// THEN
		assertThat(expectedKey).isEmpty();
	}

	@Test
	void revokeKey_deletesFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		testSubject.revokeKey(mockUserId);

		// THEN
		verify(apiKeyRepository).deleteByUserId(mockUserId);
	}

	@Test
	void revokeKey_invalidatesCache_soSubsequentLookupHitsDb() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		ApiKey mockApiKey = Instancio.of(ApiKey.class)
			.set(Select.field(ApiKey::getUser), mockUser)
			.create();
		String mockPlainKey = "aurum_revokekey123";
		when(apiKeyRepository.findByKeyHash(org.mockito.ArgumentMatchers.notNull()))
			.thenReturn(Optional.of(mockApiKey))
			.thenReturn(Optional.empty());

		// WHEN
		testSubject.resolveUser(mockPlainKey);
		testSubject.revokeKey(mockUser.getId());
		Optional<User> expectedUser = testSubject.resolveUser(mockPlainKey);

		// THEN
		assertThat(expectedUser).isEmpty();
	}
}
