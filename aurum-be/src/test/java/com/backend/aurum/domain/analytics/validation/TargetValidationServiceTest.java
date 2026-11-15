package com.backend.aurum.domain.analytics.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import com.backend.aurum.domain.shared.exception.ValidationException;
import java.math.BigDecimal;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TargetValidationServiceTest {

	private static final int MAX_NAME_LENGTH = 100;

	private static final String BLANK_NAME = "   ";

	private static final String VALID_NAME = "Financial independence";

	private static final BigDecimal VALID_AMOUNT = new BigDecimal("1000");

	private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-1");

	private static final String NAME_FIELD = "name";

	private static final String AMOUNT_FIELD = "targetAmount";

	private static final String NO_DATA_MESSAGE = "Target data is required";

	private static final String MISSING_NAME_MESSAGE = "Target name is required";

	private static final String NAME_TOO_LONG_MESSAGE = "Target name cannot exceed 100 characters";

	private static final String NON_POSITIVE_AMOUNT_MESSAGE =
		"Target amount must be greater than zero";

	@InjectMocks
	private TargetValidationService testSubject;

	@ParameterizedTest(name = "{0}")
	@MethodSource("createRejections")
	void validate_createDto_answersThePinnedFieldAndMessage(CreateRejection createRejection) {
		// GIVEN
		CreateTargetDTO mockDto = createRejection.dto();

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
				"400 without a field when the target data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new CreateRejection(
				"400 on the name when it is blank",
				createDtoWith(dto -> dto.setName(BLANK_NAME)),
				NAME_FIELD,
				MISSING_NAME_MESSAGE
			),
			new CreateRejection(
				"400 on the name when it is longer than the maximum",
				createDtoWith(dto -> dto.setName(tooLongName())),
				NAME_FIELD,
				NAME_TOO_LONG_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it is missing",
				createDtoWith(dto -> dto.setTargetAmount(null)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it is zero",
				createDtoWith(dto -> dto.setTargetAmount(ZERO_AMOUNT)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it is negative",
				createDtoWith(dto -> dto.setTargetAmount(NEGATIVE_AMOUNT)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("updateRejections")
	void validate_updateDto_answersThePinnedFieldAndMessage(UpdateRejection updateRejection) {
		// GIVEN
		UpdateTargetDTO mockDto = updateRejection.dto();

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
				"400 without a field when the target data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new UpdateRejection(
				"400 on the name when it is blank",
				updateDtoWith(dto -> dto.setName(BLANK_NAME)),
				NAME_FIELD,
				MISSING_NAME_MESSAGE
			),
			new UpdateRejection(
				"400 on the name when it is longer than the maximum",
				updateDtoWith(dto -> dto.setName(tooLongName())),
				NAME_FIELD,
				NAME_TOO_LONG_MESSAGE
			),
			new UpdateRejection(
				"400 on the amount when it is missing",
				updateDtoWith(dto -> dto.setTargetAmount(null)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			),
			new UpdateRejection(
				"400 on the amount when it is zero",
				updateDtoWith(dto -> dto.setTargetAmount(ZERO_AMOUNT)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			),
			new UpdateRejection(
				"400 on the amount when it is negative",
				updateDtoWith(dto -> dto.setTargetAmount(NEGATIVE_AMOUNT)),
				AMOUNT_FIELD,
				NON_POSITIVE_AMOUNT_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedCreateDtos")
	void validate_createDto_acceptsTheDto(AcceptedCreateDto acceptedCreateDto) {
		// GIVEN
		CreateTargetDTO mockDto = acceptedCreateDto.dto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	static Stream<AcceptedCreateDto> acceptedCreateDtos() {
		return Stream.of(
			new AcceptedCreateDto(
				"the name sits exactly at the maximum length",
				createDtoWith(dto -> dto.setName(maxLengthName()))
			)
		);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("acceptedUpdateDtos")
	void validate_updateDto_acceptsTheDto(AcceptedUpdateDto acceptedUpdateDto) {
		// GIVEN
		UpdateTargetDTO mockDto = acceptedUpdateDto.dto();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	static Stream<AcceptedUpdateDto> acceptedUpdateDtos() {
		return Stream.of(
			new AcceptedUpdateDto(
				"the name sits exactly at the maximum length",
				updateDtoWith(dto -> dto.setName(maxLengthName()))
			)
		);
	}

	private static String tooLongName() {
		return "a".repeat(MAX_NAME_LENGTH + 1);
	}

	private static String maxLengthName() {
		return "a".repeat(MAX_NAME_LENGTH);
	}

	private static CreateTargetDTO createDtoWith(Consumer<CreateTargetDTO> mutation) {
		CreateTargetDTO dto = new CreateTargetDTO();
		dto.setName(VALID_NAME);
		dto.setTargetAmount(VALID_AMOUNT);
		mutation.accept(dto);
		return dto;
	}

	private static UpdateTargetDTO updateDtoWith(Consumer<UpdateTargetDTO> mutation) {
		UpdateTargetDTO dto = new UpdateTargetDTO();
		dto.setName(VALID_NAME);
		dto.setTargetAmount(VALID_AMOUNT);
		mutation.accept(dto);
		return dto;
	}

	private record CreateRejection(
		String scenario,
		CreateTargetDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record UpdateRejection(
		String scenario,
		UpdateTargetDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record AcceptedCreateDto(String scenario, CreateTargetDTO dto) {}

	private record AcceptedUpdateDto(String scenario, UpdateTargetDTO dto) {}
}
