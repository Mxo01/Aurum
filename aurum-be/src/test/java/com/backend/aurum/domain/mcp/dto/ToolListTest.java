package com.backend.aurum.domain.mcp.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ToolListTest {

	private static final int LIST_CAP = 3;
	private static final int UNDER_THE_CAP = LIST_CAP - 1;
	private static final int AT_THE_CAP = LIST_CAP;
	private static final int OVER_THE_CAP = LIST_CAP + 2;

	@ParameterizedTest(name = "{0}")
	@MethodSource("cappedLists")
	void capsTheItemsAndStillReportsTheFullCount(
		String scenario,
		int totalCount,
		int expectedItemCount,
		boolean expectedTruncated
	) {
		// GIVEN
		List<Integer> stubbedItems = items(totalCount);

		// WHEN
		ToolList<Integer> testSubject = ToolList.cappedAt(stubbedItems, LIST_CAP);

		// THEN
		assertThat(testSubject.items()).containsExactlyElementsOf(
			stubbedItems.subList(0, expectedItemCount)
		);
		assertThat(testSubject.totalCount()).isEqualTo(totalCount);
		assertThat(testSubject.truncated()).isEqualTo(expectedTruncated);
	}

	private static Stream<Arguments> cappedLists() {
		return Stream.of(
			Arguments.of("a list under the cap is returned whole", UNDER_THE_CAP, UNDER_THE_CAP, false),
			Arguments.of("a list exactly at the cap is returned whole", AT_THE_CAP, AT_THE_CAP, false),
			Arguments.of(
				"a list over the cap keeps the first items and reports the rest",
				OVER_THE_CAP,
				LIST_CAP,
				true
			)
		);
	}

	private static List<Integer> items(int count) {
		return IntStream.range(0, count).boxed().toList();
	}
}
