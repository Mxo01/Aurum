package com.backend.aurum.domain.mcp.service;

import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.repository.ApiKeyRepository;
import com.backend.aurum.domain.user.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

	private static final String KEY_PREFIX = "aurum_";
	private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

	private record CacheEntry(User user, Instant expiresAt) {}

	private final ApiKeyRepository apiKeyRepository;

	/** keyHash → cached user */
	private final ConcurrentHashMap<String, CacheEntry> keyCache = new ConcurrentHashMap<>();

	/** userId → keyHash (reverse index for O(1) eviction) */
	private final ConcurrentHashMap<UUID, String> userHashIndex = new ConcurrentHashMap<>();

	@Transactional
	public String generateKey(User user) {
		evictByUserId(user.getId());
		apiKeyRepository.deleteByUserId(user.getId());

		String plainKey = KEY_PREFIX + generateToken();
		String hash = hashKey(plainKey);

		ApiKey apiKey = ApiKey.builder()
			.user(user)
			.keyHash(hash)
			.name("default")
			.createdAt(LocalDateTime.now())
			.build();

		apiKeyRepository.save(Objects.requireNonNull(apiKey));
		return plainKey;
	}

	@Transactional
	public Optional<User> resolveUser(String plainKey) {
		String hash = hashKey(plainKey);
		CacheEntry cached = keyCache.get(hash);
		if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
			return Optional.of(cached.user());
		}

		return resolveFromDb(hash);
	}

	public Optional<ApiKey> getKeyMeta(UUID userId) {
		return apiKeyRepository.findByUserId(userId).stream().findFirst();
	}

	@Transactional
	public void revokeKey(UUID userId) {
		evictByUserId(userId);
		apiKeyRepository.deleteByUserId(userId);
	}

	private Optional<User> resolveFromDb(String hash) {
		return apiKeyRepository
			.findByKeyHash(hash)
			.map(key -> {
				key.setLastUsedAt(LocalDateTime.now());
				User user = key.getUser();
				Instant expiresAt = Instant.now().plusSeconds(CACHE_TTL_SECONDS);
				keyCache.put(hash, new CacheEntry(user, expiresAt));
				userHashIndex.put(user.getId(), hash);
				return user;
			});
	}

	private void evictByUserId(UUID userId) {
		String oldHash = userHashIndex.remove(userId);
		if (oldHash != null) keyCache.remove(oldHash);
	}

	private String hashKey(String plainKey) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(plainKey.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	private String generateToken() {
		byte[] bytes = new byte[24];
		new SecureRandom().nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
