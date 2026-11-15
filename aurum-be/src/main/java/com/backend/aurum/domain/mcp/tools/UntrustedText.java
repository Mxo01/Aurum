package com.backend.aurum.domain.mcp.tools;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Neutralises user text before it leaves a tool.
 *
 * <p>An asset name is written by whoever authored the statement a user imported, and it lands
 * in somebody else's model context as ordinary tool output. A newline is what lets such a
 * value escape its own line and pose as a new section or a new instruction, so control and
 * whitespace characters collapse into one space.
 *
 * <p>Bidi and zero-width marks are removed rather than replaced: inside a word they are
 * invisible (ZWNJ, ZWJ, RLM), so a space in their place would silently split it.
 */
public final class UntrustedText {

	public static final int MAX_LENGTH = 100;

	private static final Pattern INVISIBLE = Pattern.compile("\\p{Cf}");
	private static final Pattern STRIPPED = Pattern.compile("[\\p{Cc}\\s]+");
	private static final String COLLAPSED_SPACE = " ";

	private UntrustedText() {}

	public static String clean(String raw) {
		if (raw == null) {
			return null;
		}
		String visible = INVISIBLE.matcher(raw).replaceAll("");
		String singleLine = STRIPPED.matcher(visible).replaceAll(COLLAPSED_SPACE).strip();
		return cap(singleLine, MAX_LENGTH);
	}

	/**
	 * Cleans every key of a map named by the user.
	 *
	 * <p>Keys that collide once cleaned keep a numbered suffix instead of overwriting each
	 * other: a bucket silently merged or dropped would misreport the allocation, and the
	 * suffix is derived from the input order so the same map always yields the same keys.
	 */
	public static <V> Map<String, V> cleanKeys(Map<String, V> raw) {
		if (raw == null) {
			return null;
		}
		Map<String, V> cleaned = new LinkedHashMap<>(raw.size());
		for (Map.Entry<String, V> entry : raw.entrySet()) {
			cleaned.put(unique(clean(entry.getKey()), cleaned), entry.getValue());
		}
		return cleaned;
	}

	private static String unique(String base, Map<String, ?> taken) {
		String key = base == null ? "" : base;
		if (!taken.containsKey(key)) {
			return key;
		}
		int suffix = 2;
		while (taken.containsKey(withSuffix(key, suffix))) {
			suffix++;
		}
		return withSuffix(key, suffix);
	}

	private static String withSuffix(String base, int suffix) {
		String marker = " (" + suffix + ")";
		return cap(base, MAX_LENGTH - marker.length()) + marker;
	}

	private static String cap(String text, int maxCodePoints) {
		if (text.codePointCount(0, text.length()) <= maxCodePoints) {
			return text;
		}
		return text.substring(0, text.offsetByCodePoints(0, maxCodePoints)).strip();
	}
}
