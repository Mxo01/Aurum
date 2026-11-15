package com.backend.aurum.domain.shared.logging;

/**
 * Makes user-controlled text safe to place in a log line: a newline in a free-text field would
 * otherwise let the caller forge an entry, and an unbounded value would flood the file.
 */
public final class LogSafe {

	private static final int MAX_LENGTH = 120;
	private static final String TRUNCATION_MARKER = "…";

	private LogSafe() {}

	public static String text(String value) {
		if (value == null) {
			return null;
		}
		StringBuilder sanitized = new StringBuilder(Math.min(value.length(), MAX_LENGTH));
		boolean truncated = false;
		for (int index = 0; index < value.length(); index++) {
			char character = value.charAt(index);
			if (Character.isISOControl(character)) {
				continue;
			}
			if (sanitized.length() == MAX_LENGTH) {
				truncated = true;
				break;
			}
			sanitized.append(character);
		}
		return truncated ? sanitized.append(TRUNCATION_MARKER).toString() : sanitized.toString();
	}
}
