package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCategoryValidationServiceTest {

	@InjectMocks
	private AssetCategoryValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN
		CreateAssetCategoryDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenNameIsBlank() {
		// GIVEN
		CreateAssetCategoryDTO mockDto = Instancio.of(CreateAssetCategoryDTO.class)
			.set(Select.field(CreateAssetCategoryDTO::getName), "")
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenTypeIsNull() {
		// GIVEN
		CreateAssetCategoryDTO mockDto = Instancio.of(CreateAssetCategoryDTO.class)
			.set(Select.field(CreateAssetCategoryDTO::getType), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_doesNotThrowForValidDto() {
		// GIVEN
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}

	@Test
	void validate_updateDto_throwsWhenDtoIsNull() {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = null;

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenNameIsBlank() {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = Instancio.of(UpdateAssetCategoryDTO.class)
			.set(Select.field(UpdateAssetCategoryDTO::getName), "")
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_throwsWhenTypeIsNull() {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = Instancio.of(UpdateAssetCategoryDTO.class)
			.set(Select.field(UpdateAssetCategoryDTO::getType), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_updateDto_doesNotThrowForValidDto() {
		// GIVEN
		UpdateAssetCategoryDTO mockDto = Instancio.create(UpdateAssetCategoryDTO.class);

		// WHEN / THEN
		assertThatCode(() -> testSubject.validate(mockDto)).doesNotThrowAnyException();
	}
}
