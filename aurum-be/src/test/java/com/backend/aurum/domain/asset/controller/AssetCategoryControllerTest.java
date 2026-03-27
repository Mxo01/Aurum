package com.backend.aurum.domain.asset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.facade.AssetCategoryFacade;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import java.util.List;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AssetCategoryControllerTest {

	@Mock
	private AssetCategoryFacade categoryFacade;

	@InjectMocks
	private AssetCategoryController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getAllCategories_returnsOkWithDtosFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<AssetCategoryDTO> stubbedDtos = Instancio.ofList(AssetCategoryDTO.class).size(2).create();
		when(categoryFacade.getAllCategories(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<AssetCategoryDTO>> expectedResponse = testSubject.getAllCategories(
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void createCategory_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(categoryFacade.createCategory(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetCategoryDTO> expectedResponse = testSubject.createCategory(
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void updateCategory_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		UpdateAssetCategoryDTO mockDto = Instancio.create(UpdateAssetCategoryDTO.class);
		AssetCategoryDTO stubbedDto = Instancio.create(AssetCategoryDTO.class);
		when(categoryFacade.updateCategory(mockId, mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetCategoryDTO> expectedResponse = testSubject.updateCategory(
			mockId,
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void deleteCategory_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteCategory(mockId, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(categoryFacade).deleteCategory(mockId, mockUserId);
	}
}
