package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetValidationServiceTest {

	private static final int NAME_MAX_LENGTH = 100;

	@InjectMocks
	private AssetValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN
		CreateAssetDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("invalidCreateDtos")
	void validate_createDto_rejectsInvalidPayload(
		String scenario,
		Consumer<CreateAssetDTO> mutation
	) {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> invalidCreateDtos() {
		return Stream.of(
			Arguments.of("blank name", (Consumer<CreateAssetDTO>) dto -> dto.setName("")),
			Arguments.of("null name", (Consumer<CreateAssetDTO>) dto -> dto.setName(null)),
			Arguments.of(
				"name over max length",
				(Consumer<CreateAssetDTO>) dto -> dto.setName("a".repeat(NAME_MAX_LENGTH + 1))
			),
			Arguments.of("null category", (Consumer<CreateAssetDTO>) dto -> dto.setCategoryId(null))
		);
	}

	@Test
	void validate_createDto_doesNotThrowForValidDto() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getName), "a".repeat(NAME_MAX_LENGTH))
			.create();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_updateDto_throwsWhenDtoIsNull() {
		// GIVEN
		UpdateAssetDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("invalidUpdateDtos")
	void validate_updateDto_rejectsInvalidPayload(
		String scenario,
		Consumer<UpdateAssetDTO> mutation
	) {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> invalidUpdateDtos() {
		return Stream.of(
			Arguments.of("blank name", (Consumer<UpdateAssetDTO>) dto -> dto.setName("")),
			Arguments.of("null name", (Consumer<UpdateAssetDTO>) dto -> dto.setName(null)),
			Arguments.of(
				"name over max length",
				(Consumer<UpdateAssetDTO>) dto -> dto.setName("a".repeat(NAME_MAX_LENGTH + 1))
			)
		);
	}

	@Test
	void validate_updateDto_doesNotThrowForValidDto() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getName), "a".repeat(NAME_MAX_LENGTH))
			.create();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}
}
