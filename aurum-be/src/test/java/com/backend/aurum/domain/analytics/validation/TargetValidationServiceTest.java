package com.backend.aurum.domain.analytics.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.analytics.dto.CreateTargetDTO;
import com.backend.aurum.domain.analytics.dto.UpdateTargetDTO;
import java.math.BigDecimal;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TargetValidationServiceTest {

	@InjectMocks
	private TargetValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN
		CreateTargetDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenNameIsBlank() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getName), "")
			.set(Select.field(CreateTargetDTO::getTargetAmount), BigDecimal.TEN)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenTargetAmountIsNull() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getTargetAmount), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenTargetAmountIsZero() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getTargetAmount), BigDecimal.ZERO)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenTargetAmountIsNegative() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getTargetAmount), new BigDecimal("-100"))
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_doesNotThrowForValidDto() {
		// GIVEN
		CreateTargetDTO mockDto = Instancio.of(CreateTargetDTO.class)
			.set(Select.field(CreateTargetDTO::getTargetAmount), BigDecimal.TEN)
			.create();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_updateDto_throwsWhenDtoIsNull() {
		// GIVEN
		UpdateTargetDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenNameIsBlank() {
		// GIVEN
		UpdateTargetDTO mockDto = Instancio.of(UpdateTargetDTO.class)
			.set(Select.field(UpdateTargetDTO::getName), "  ")
			.set(Select.field(UpdateTargetDTO::getTargetAmount), BigDecimal.TEN)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenTargetAmountIsZero() {
		// GIVEN
		UpdateTargetDTO mockDto = Instancio.of(UpdateTargetDTO.class)
			.set(Select.field(UpdateTargetDTO::getTargetAmount), BigDecimal.ZERO)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_doesNotThrowForValidDto() {
		// GIVEN
		UpdateTargetDTO mockDto = Instancio.of(UpdateTargetDTO.class)
			.set(Select.field(UpdateTargetDTO::getTargetAmount), BigDecimal.TEN)
			.create();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}
}
