package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetValidationServiceTest {

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

	@Test
	void validate_createDto_throwsWhenNameIsBlank() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getName), "")
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenNameIsNull() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getName), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenCategoryIdIsNull() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenNameExceedsMaxLength() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getName), "a".repeat(101))
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_doesNotThrowForValidDto() {
		// GIVEN
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getName), "a".repeat(100))
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

	@Test
	void validate_updateDto_throwsWhenNameIsBlank() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getName), "")
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenNameIsNull() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getName), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenNameExceedsMaxLength() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getName), "a".repeat(101))
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_doesNotThrowForValidDto() {
		// GIVEN
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getName), "a".repeat(100))
			.create();

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}
}
