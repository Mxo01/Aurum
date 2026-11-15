package com.backend.aurum.domain.asset.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.backend.aurum.domain.user.enums.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("liabilityTypes")
	void isLiability_isTrue_whenTheLiabilityTypeIsSet(String scenario, LiabilityType liabilityType) {
		// GIVEN
		Asset testSubject = buildAsset(liabilityType);

		// WHEN
		boolean expectedLiability = testSubject.isLiability();

		// THEN
		assertThat(expectedLiability).isTrue();
	}

	static Stream<Arguments> liabilityTypes() {
		return Stream.of(
			Arguments.of("a manual liability", LiabilityType.MANUAL),
			Arguments.of("an automatic liability", LiabilityType.AUTOMATIC)
		);
	}

	@Test
	void isLiability_isFalse_whenTheLiabilityTypeIsAbsent() {
		// GIVEN
		Asset testSubject = buildAsset(null);

		// WHEN
		boolean expectedLiability = testSubject.isLiability();

		// THEN
		assertThat(expectedLiability).isFalse();
	}

	@Test
	void isLiability_staysOutOfTheSerializedJson() throws JsonProcessingException {
		// GIVEN
		Asset testSubject = buildAsset(LiabilityType.AUTOMATIC);

		// WHEN
		JsonNode expectedJson = SERIALIZER.readTree(SERIALIZER.writeValueAsString(testSubject));

		// THEN
		assertThat(expectedJson.has(LIABILITY_TYPE_PROPERTY)).isTrue();
		assertThat(expectedJson.has(LIABILITY_PROPERTY)).isFalse();
	}

	private Asset buildAsset(LiabilityType liabilityType) {
		Asset mockAsset = new Asset();
		mockAsset.setName(ASSET_NAME);
		mockAsset.setOriginalCurrency(Currency.EUR);
		mockAsset.setLiabilityType(liabilityType);
		return mockAsset;
	}
}
