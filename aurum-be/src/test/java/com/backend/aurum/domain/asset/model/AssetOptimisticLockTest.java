package com.backend.aurum.domain.asset.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.Column;
import jakarta.persistence.Version;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class AssetOptimisticLockTest {

	private static final String VERSION_FIELD = "version";

	private static final String MASTER_CHANGELOG_RESOURCE = "/db/changelog/db.changelog-master.yaml";

	private static final String VERSION_CHANGESET_FILE =
		"db/changelog/changes/2026-11-15-add-asset-optimistic-lock.sql";

	private static final String VERSION_CHANGESET_RESOURCE = "/" + VERSION_CHANGESET_FILE;

	private static final String CHANGELOG_INCLUDE_FILE_KEY = "file: " + VERSION_CHANGESET_FILE;

	private static final String CHANGELOG_RELATIVE_TO_KEY = "relativeToChangelogFile: false";

	private static final String EXPECTED_COLUMN_DEFINITION =
		"ALTER TABLE assets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;";

	private static final String EXPECTED_COLUMN_ROLLBACK =
		"ALTER TABLE assets DROP COLUMN IF EXISTS version;";

	private static final String ROLLBACK_KEY = "--rollback";

	@Test
	void versionField_isTheOptimisticLockOfTheAsset() throws NoSuchFieldException {
		// GIVEN
		Class<?> testSubject = Asset.class;

		// WHEN
		Field expectedVersionField = testSubject.getDeclaredField(VERSION_FIELD);

		// THEN
		assertThat(expectedVersionField.getType()).isEqualTo(Long.class);
		assertThat(expectedVersionField.isAnnotationPresent(Version.class)).isTrue();
	}

	@Test
	void versionField_isMappedToANonNullableColumn() throws NoSuchFieldException {
		// GIVEN
		Class<?> testSubject = Asset.class;

		// WHEN
		Column expectedColumn = testSubject.getDeclaredField(VERSION_FIELD).getAnnotation(Column.class);

		// THEN
		assertThat(expectedColumn).isNotNull();
		assertThat(expectedColumn.nullable()).isFalse();
	}

	@Test
	void masterChangelog_includesTheVersionChangesetFromTheClasspathRoot() throws IOException {
		// GIVEN
		String mockChangelogResource = MASTER_CHANGELOG_RESOURCE;

		// WHEN
		List<String> expectedChangelogLines = linesOf(mockChangelogResource);
		int actualIncludeIndex = indexOfLineContaining(
			expectedChangelogLines,
			CHANGELOG_INCLUDE_FILE_KEY
		);

		// THEN
		assertThat(actualIncludeIndex).isNotNegative();
		assertThat(nextNonBlankLine(expectedChangelogLines, actualIncludeIndex)).isEqualTo(
			CHANGELOG_RELATIVE_TO_KEY
		);
	}

	@Test
	void versionChangeset_addsTheVersionColumnWithADefault() throws IOException {
		// GIVEN
		String mockChangesetResource = VERSION_CHANGESET_RESOURCE;

		// WHEN
		String expectedChangeset = textOf(mockChangesetResource);

		// THEN
		assertThat(expectedChangeset).contains(EXPECTED_COLUMN_DEFINITION);
	}

	@Test
	void versionChangeset_rollsBackByDroppingTheVersionColumn() throws IOException {
		// GIVEN
		String mockChangesetResource = VERSION_CHANGESET_RESOURCE;

		// WHEN
		String expectedChangeset = textOf(mockChangesetResource);

		// THEN
		assertThat(expectedChangeset).contains(ROLLBACK_KEY + " " + EXPECTED_COLUMN_ROLLBACK);
	}

	private static int indexOfLineContaining(List<String> changelogLines, String expectedLine) {
		for (int lineIndex = 0; lineIndex < changelogLines.size(); lineIndex++) {
			if (changelogLines.get(lineIndex).trim().equals(expectedLine)) {
				return lineIndex;
			}
		}
		return -1;
	}

	private static String nextNonBlankLine(List<String> changelogLines, int fromIndex) {
		for (int lineIndex = fromIndex + 1; lineIndex < changelogLines.size(); lineIndex++) {
			String candidateLine = changelogLines.get(lineIndex).trim();
			if (!candidateLine.isEmpty()) {
				return candidateLine;
			}
		}
		return "";
	}

	private static List<String> linesOf(String resource) throws IOException {
		return textOf(resource).lines().toList();
	}

	private static String textOf(String resource) throws IOException {
		try (InputStream resourceStream = AssetOptimisticLockTest.class.getResourceAsStream(resource)) {
			assertThat(resourceStream).isNotNull();
			return new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
