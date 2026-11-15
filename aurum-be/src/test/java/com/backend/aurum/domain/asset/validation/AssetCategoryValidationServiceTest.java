package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCategoryValidationServiceTest {

	private static final int MAX_NAME_LENGTH = 100;
	private static final int MAX_ICON_LENGTH = 50;
	private static final String BLANK_NAME = "   ";
	private static final String VALID_NAME = "Travel";
	private static final String VALID_ICON = "pi-bitcoin";
	private static final String NAME_FIELD = "name";
	private static final String ICON_FIELD = "icon";
	private static final String TYPE_FIELD = "type";
	private static final String NO_DATA_MESSAGE = "Category data is required";
	private static final String MISSING_NAME_MESSAGE = "Category name is required";
	private static final String NAME_TOO_LONG_MESSAGE = "Category name cannot exceed 100 characters";
	private static final String ICON_TOO_LONG_MESSAGE = "Category icon cannot exceed 50 characters";
	private static final String MISSING_TYPE_MESSAGE = "Category type is required";

	@InjectMocks
	private AssetCategoryValidationService testSubject;

	@ParameterizedTest(name = "{0}")
	@MethodSource("createRejections")
	void validate_createDto_answersThePinnedFieldAndMessage(CreateRejection createRejection) {
		// GIVEN
		CreateAssetCategoryDTO mockDto = createRejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOfSatisfying(
			ValidationException.class,
			expectedException -> {
				assertThat(expectedException.getField()).isEqualTo(createRejection.expectedField());
				assertThat(expectedException.getMessage()).isEqualTo(createRejection.expectedMessage());
			}
		);
	}

	static Stream<CreateRejection> createRejections() {
		return Stream.of(
			new CreateRejection(
				"400 without a field when the category data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new CreateRejection(
				"400 on the name when it is blank",
				createDto(BLANK_NAME, AssetType.ASSET, VALID_ICON),
				NAME_FIELD,
				MISSING_NAME_MESSAGE
			),
			new CreateRejection(
				"400 on the name when it is longer than the maximum",
				createDto(tooLongName(), AssetType.ASSET, VALID_ICON),
				NAME_FIELD,
				NAME_TOO_LONG_MESSAGE
			),
			new CreateRejection(
				"400 on the icon when it is longer than the maximum",
				createDto(VALID_NAME, AssetType.ASSET, tooLongIcon()),
				ICON_FIELD,
				ICON_TOO_LONG_MESSAGE
			),
			new CreateRejection(
				"400 on the type when it is missing",
				createDto(VALID_NAME, null, VALID_ICON),
				TYPE_FIELD,
				MISSING_TYPE_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("updateRejections")
	void validate_updateDto_answersThePinnedFieldAndMessage(UpdateRejection updateRejection) {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = updateRejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOfSatisfying(
			ValidationException.class,
			expectedException -> {
				assertThat(expectedException.getField()).isEqualTo(updateRejection.expectedField());
				assertThat(expectedException.getMessage()).isEqualTo(updateRejection.expectedMessage());
			}
		);
	}

	static Stream<UpdateRejection> updateRejections() {
		return Stream.of(
			new UpdateRejection(
				"400 without a field when the category data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new UpdateRejection(
				"400 on the name when it is blank",
				updateDto(BLANK_NAME, AssetType.ASSET, VALID_ICON),
				NAME_FIELD,
				MISSING_NAME_MESSAGE
			),
			new UpdateRejection(
				"400 on the name when it is longer than the maximum",
				updateDto(tooLongName(), AssetType.ASSET, VALID_ICON),
				NAME_FIELD,
				NAME_TOO_LONG_MESSAGE
			),
			new UpdateRejection(
				"400 on the icon when it is longer than the maximum",
				updateDto(VALID_NAME, AssetType.ASSET, tooLongIcon()),
				ICON_FIELD,
				ICON_TOO_LONG_MESSAGE
			),
			new UpdateRejection(
				"400 on the type when it is missing",
				updateDto(VALID_NAME, null, VALID_ICON),
				TYPE_FIELD,
				MISSING_TYPE_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedCreateDtos")
	void validate_createDto_acceptsTheDto(AcceptedCreateDto acceptedCreateDto) {
		// GIVEN
		CreateAssetCategoryDTO mockDto = acceptedCreateDto.dto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	static Stream<AcceptedCreateDto> acceptedCreateDtos() {
		return Stream.of(
			new AcceptedCreateDto(
				"the name sits exactly at the maximum length",
				createDto(maxLengthName(), AssetType.ASSET, VALID_ICON)
			),
			new AcceptedCreateDto(
				"the category carries no icon",
				createDto(VALID_NAME, AssetType.LIABILITY, null)
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedUpdateDtos")
	void validate_updateDto_acceptsTheDto(AcceptedUpdateDto acceptedUpdateDto) {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = acceptedUpdateDto.dto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	static Stream<AcceptedUpdateDto> acceptedUpdateDtos() {
		return Stream.of(
			new AcceptedUpdateDto(
				"the name sits exactly at the maximum length",
				updateDto(maxLengthName(), AssetType.ASSET, VALID_ICON)
			),
			new AcceptedUpdateDto(
				"the category carries no icon",
				updateDto(VALID_NAME, AssetType.LIABILITY, null)
			)
		);
	}

	private static String tooLongName() {
		return "a".repeat(MAX_NAME_LENGTH + 1);
	}

	private static String maxLengthName() {
		return "a".repeat(MAX_NAME_LENGTH);
	}

	private static String tooLongIcon() {
		return "a".repeat(MAX_ICON_LENGTH + 1);
	}

	private static CreateAssetCategoryDTO createDto(String name, AssetType type, String icon) {
		CreateAssetCategoryDTO mockDto = new CreateAssetCategoryDTO();
		mockDto.setName(name);
		mockDto.setType(type);
		mockDto.setIcon(icon);
		return mockDto;
	}

	private static UpdateAssetCategoryDTO updateDto(String name, AssetType type, String icon) {
		UpdateAssetCategoryDTO mockDto = new UpdateAssetCategoryDTO();
		mockDto.setName(name);
		mockDto.setType(type);
		mockDto.setIcon(icon);
		return mockDto;
	}

	private record CreateRejection(
		String scenario,
		CreateAssetCategoryDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record UpdateRejection(
		String scenario,
		UpdateAssetCategoryDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record AcceptedCreateDto(String scenario, CreateAssetCategoryDTO dto) {}

	private record AcceptedUpdateDto(String scenario, UpdateAssetCategoryDTO dto) {}
}
