package com.backend.aurum.domain.asset.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.user.enums.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AssetTest {

	private static final ObjectMapper SERIALIZER = new ObjectMapper().findAndRegisterModules();

	private static final String LIABILITY_PROPERTY = "liability";

	private static final String LIABILITY_TYPE_PROPERTY = "liabilityType";

	private static final String ASSET_NAME = "Mortgage";

	private static final String ASSET_CATEGORY_NAME = "Property";

	private static final String LIABILITY_CATEGORY_NAME = "Debts";

	private static final List<AssetType> CATEGORY_TYPES = Arrays.asList(
		AssetType.ASSET,
		AssetType.LIABILITY,
		null
	);

	private static final List<LiabilityType> LIABILITY_TYPES = Arrays.asList(
		null,
		LiabilityType.MANUAL,
		LiabilityType.AUTOMATIC
	);

	@ParameterizedTest(name = "{0}")
	@MethodSource("classificationCases")
	void isLiability_isTrueOnly_whenTheCategoryIsALiability(
		String scenario,
		AssetType categoryType,
		LiabilityType liabilityType,
		boolean expectedLiability
	) {
		// GIVEN
		Asset testSubject = buildAsset(categoryType, liabilityType);

		// WHEN
		boolean actualLiability = testSubject.isLiability();

		// THEN
		assertThat(actualLiability).isEqualTo(expectedLiability);
	}

	static Stream<Arguments> classificationCases() {
		return CATEGORY_TYPES.stream().flatMap(categoryType ->
			LIABILITY_TYPES.stream().map(liabilityType ->
				Arguments.of(
					scenarioName(categoryType, liabilityType),
					categoryType,
					liabilityType,
					categoryType == AssetType.LIABILITY
				)
			)
		);
	}

	private static String scenarioName(AssetType categoryType, LiabilityType liabilityType) {
		return "category.type " + categoryType + " with liabilityType " + liabilityType;
	}

	@Test
	void isLiability_staysOutOfTheSerializedJson() throws JsonProcessingException {
		// GIVEN
		Asset testSubject = buildAsset(AssetType.LIABILITY, LiabilityType.AUTOMATIC);

		// WHEN
		JsonNode expectedJson = SERIALIZER.readTree(SERIALIZER.writeValueAsString(testSubject));

		// THEN
		assertThat(expectedJson.has(LIABILITY_TYPE_PROPERTY)).isTrue();
		assertThat(expectedJson.has(LIABILITY_PROPERTY)).isFalse();
	}

	private Asset buildAsset(AssetType categoryType, LiabilityType liabilityType) {
		Asset mockAsset = new Asset();
		mockAsset.setName(ASSET_NAME);
		mockAsset.setOriginalCurrency(Currency.EUR);
		mockAsset.setCategory(categoryType == null ? null : buildCategory(categoryType));
		mockAsset.setLiabilityType(liabilityType);
		return mockAsset;
	}

	private AssetCategory buildCategory(AssetType type) {
		AssetCategory mockCategory = new AssetCategory();
		mockCategory.setName(
			type == AssetType.LIABILITY ? LIABILITY_CATEGORY_NAME : ASSET_CATEGORY_NAME
		);
		mockCategory.setType(type);
		return mockCategory;
	}
}
