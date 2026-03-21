package com.backend.aurum.domain.mcp.service;

import com.backend.aurum.domain.mcp.model.ApiKey;
import com.backend.aurum.domain.mcp.repository.ApiKeyRepository;
import com.backend.aurum.domain.user.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApiKeyService {

	private final ApiKeyRepository apiKeyRepository;

	private static final String KEY_PREFIX = "aurum_";

	@Transactional
	public String generateKey(User user) {
		apiKeyRepository.deleteByUserId(user.getId());

		String plainKey = KEY_PREFIX + generateToken();

		ApiKey apiKey = ApiKey.builder()
			.user(user)
			.keyHash(hashKey(plainKey))
			.name("default")
			.createdAt(LocalDateTime.now())
			.build();

		apiKeyRepository.save(Objects.requireNonNull(apiKey));
		return plainKey;
	}

	@Transactional
	public Optional<User> resolveUser(String plainKey) {
		String hash = hashKey(plainKey);
		return apiKeyRepository
			.findByKeyHash(hash)
			.map(key -> {
				key.setLastUsedAt(LocalDateTime.now());
				return key.getUser();
			});
	}

	public Optional<ApiKey> getKeyMeta(UUID userId) {
		return apiKeyRepository.findByUserId(userId).stream().findFirst();
	}

	@Transactional
	public void revokeKey(UUID userId) {
		apiKeyRepository.deleteByUserId(userId);
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
