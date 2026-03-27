package com.backend.aurum.domain.asset.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.mapper.AssetCategoryMapper;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.service.AssetCategoryService;
import com.backend.aurum.domain.asset.validation.AssetCategoryValidationService;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCategoryFacadeTest {

	@Mock
	private AssetCategoryService categoryService;

	@Mock
	private AssetCategoryMapper mapper;

	@Mock
	private AssetCategoryValidationService validationService;

	@InjectMocks
	private AssetCategoryFacade testSubject;

	@Test
	void getAllCategories_returnsMappedDtos() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<AssetCategory> stubbedCategories = Instancio.ofList(AssetCategory.class).size(2).create();
		List<AssetCategoryDTO> stubbedDtos = Instancio.ofList(AssetCategoryDTO.class).size(2).create();
		when(categoryService.findAll(mockUserId)).thenReturn(stubbedCategories);
		when(mapper.toDto(stubbedCategories.get(0))).thenReturn(stubbedDtos.get(0));
		when(mapper.toDto(stubbedCategories.get(1))).thenReturn(stubbedDtos.get(1));

		// WHEN
		List<AssetCategoryDTO> expectedDtos = testSubject.getAllCategories(mockUserId);

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void createCategory_validatesAndReturnsDto() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);
		AssetCategory mockCategory = Instancio.create(AssetCategory.class);
		AssetCategory mockSaved = Instancio.create(AssetCategory.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockCategory);
		when(categoryService.save(mockCategory)).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved)).thenReturn(stubbedDto);

		// WHEN
		AssetCategoryDTO expectedDto = testSubject.createCategory(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateCategory_validatesAndReturnsDto() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetCategoryDTO mockDto = Instancio.create(UpdateAssetCategoryDTO.class);
		AssetCategory mockDetails = Instancio.create(AssetCategory.class);
		AssetCategory mockUpdated = Instancio.create(AssetCategory.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockDetails);
		when(categoryService.update(mockId, mockDetails, mockUserId)).thenReturn(mockUpdated);
		when(mapper.toDto(mockUpdated)).thenReturn(stubbedDto);

		// WHEN
		AssetCategoryDTO expectedDto = testSubject.updateCategory(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void deleteCategory_delegatesToService() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		testSubject.deleteCategory(mockId, mockUserId);

		// THEN
		verify(categoryService).delete(mockId, mockUserId);
	}
}
