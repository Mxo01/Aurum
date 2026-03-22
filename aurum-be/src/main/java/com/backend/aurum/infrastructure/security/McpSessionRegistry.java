package com.backend.aurum.infrastructure.security;

import com.backend.aurum.domain.user.model.User;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class McpSessionRegistry {

	private static final long SESSION_TTL_HOURS = 2;

	private record Entry(User user, Instant registeredAt) {}

	private final ConcurrentHashMap<String, Entry> sessions = new ConcurrentHashMap<>();

	public void register(String sessionId, User user) {
		log.debug(
			"McpSessionRegistry#register - Registering MCP session: sessionId={}, userId={}",
			sessionId,
			user.getId()
		);
		sessions.put(sessionId, new Entry(user, Instant.now()));
		log.info(
			"McpSessionRegistry#register - MCP session registered: sessionId={}, userId={}",
			sessionId,
			user.getId()
		);
	}

	public Optional<User> getUser(String sessionId) {
		log.debug("McpSessionRegistry#getUser - Looking up session: sessionId={}", sessionId);
		Entry entry = sessions.get(sessionId);
		if (entry == null) {
			log.debug("McpSessionRegistry#getUser - Session not found: sessionId={}", sessionId);
			return Optional.empty();
		}
		if (isExpired(entry)) {
			log.warn(
				"McpSessionRegistry#getUser - Session expired, removing it: sessionId={}",
				sessionId
			);
			sessions.remove(sessionId);
			return Optional.empty();
		}
		log.debug(
			"McpSessionRegistry#getUser - Session valid, resolved userId={}",
			entry.user().getId()
		);
		return Optional.of(entry.user());
	}

	public void remove(String sessionId) {
		log.debug("McpSessionRegistry#remove - Removing session: sessionId={}", sessionId);
		sessions.remove(sessionId);
		log.info("McpSessionRegistry#remove - Session removed: sessionId={}", sessionId);
	}

	@Scheduled(fixedRateString = "PT1H")
	public void evictExpiredSessions() {
		int before = sessions.size();
		sessions.entrySet().removeIf(e -> isExpired(e.getValue()));
		int removed = before - sessions.size();
		log.info(
			"McpSessionRegistry#evictExpiredSessions - Eviction run complete: {} expired session(s) removed, {} active session(s) remaining",
			removed,
			sessions.size()
		);
	}

	private boolean isExpired(Entry entry) {
		return entry.registeredAt().plusSeconds(SESSION_TTL_HOURS * 3600).isBefore(Instant.now());
	}
}
