package com.backend.aurum.domain.shared.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LogSafeTest {

	private static final int MAX_LENGTH = 120;

	private static final String TRUNCATION_MARKER = "…";

	private static final String PLAIN_TEXT = "avatar";

	private static final String LINE_BREAK = "\n";

	private static final String CARRIAGE_RETURN = "\r";

	private static final String TAB = "\t";

	private static final String NUL = "\u0000";

	private static final String FORGED_ENTRY = "avatar" + LINE_BREAK + "ERROR forged entry";

	private static final String STRIPPED_FORGED_ENTRY = "avatarERROR forged entry";

	private static final String TEXT_AT_THE_CAP = "a".repeat(MAX_LENGTH);

	private static final String TEXT_OVER_THE_CAP = "a".repeat(MAX_LENGTH + 1);

	private static final String TRUNCATED_TEXT = TEXT_AT_THE_CAP + TRUNCATION_MARKER;

	private static final String CONTROLS_AT_THE_CAP =
		TEXT_AT_THE_CAP + LINE_BREAK + CARRIAGE_RETURN + TAB + NUL;

	@ParameterizedTest(name = "{0}")
	@MethodSource("unsafeTexts")
	void text_makesTheValueSafeForALogLine(String caseName, String value, String expectedText) {
		assertThat(LogSafe.text(value)).isEqualTo(expectedText);
	}

	private static Stream<Arguments> unsafeTexts() {
		return Stream.of(
			Arguments.of("a null value stays null", null, null),
			Arguments.of("plain text is untouched", PLAIN_TEXT, PLAIN_TEXT),
			Arguments.of("a line break cannot forge an entry", FORGED_ENTRY, STRIPPED_FORGED_ENTRY),
			Arguments.of("a carriage return is stripped", "a" + CARRIAGE_RETURN + "b", "ab"),
			Arguments.of("a tab is stripped", "a" + TAB + "b", "ab"),
			Arguments.of("a nul byte is stripped", "a" + NUL + "b", "ab"),
			Arguments.of("the exact cap is kept whole", TEXT_AT_THE_CAP, TEXT_AT_THE_CAP),
			Arguments.of("one character over the cap is truncated", TEXT_OVER_THE_CAP, TRUNCATED_TEXT),
			Arguments.of(
				"control characters do not consume the cap",
				CONTROLS_AT_THE_CAP,
				TEXT_AT_THE_CAP
			)
		);
	}
}
