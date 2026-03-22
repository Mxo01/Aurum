package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.user.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class McpSessionRegistry {

	private static final long SESSION_TTL_HOURS = 2;

	private record Entry(User user, Instant registeredAt) {}

	private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();

	public void register(String sessionId, User user) {
		sessions.put(sessionId, new Entry(user, Instant.now()));
	}

	public Optional<User> getUser(String sessionId) {
		Entry entry = sessions.get(sessionId);
		if (entry == null) return Optional.empty();
		if (isExpired(entry)) {
			sessions.remove(sessionId);
			return Optional.empty();
		}
		return Optional.of(entry.user());
	}

	public void remove(String sessionId) {
		sessions.remove(sessionId);
	}

	@Scheduled(fixedRateString = "PT1H")
	public void evictExpiredSessions() {
		sessions.entrySet().removeIf(e -> isExpired(e.getValue()));
	}

	private boolean isExpired(Entry entry) {
		return entry.registeredAt().plusSeconds(SESSION_TTL_HOURS * 3600).isBefore(Instant.now());
	}
}
