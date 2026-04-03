package com.backend.aurum.domain.asset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.facade.AssetFacade;
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
class AssetControllerTest {

	@Mock
	private AssetFacade assetFacade;

	@InjectMocks
	private AssetController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getAllAssets_returnsOkWithDtosFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<AssetDTO> stubbedDtos = Instancio.ofList(AssetDTO.class).size(3).create();
		when(assetFacade.getAllAssets(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<AssetDTO>> expectedResponse = testSubject.getAllAssets(mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void getAssetById_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.getAsset(mockAssetId, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.getAssetById(
			mockAssetId,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void createAsset_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.createAsset(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.createAsset(mockDto, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void updateAsset_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetFacade.updateAssetLight(mockAssetId, mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<AssetDTO> expectedResponse = testSubject.updateAsset(
			mockAssetId,
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void deleteAsset_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteAsset(mockAssetId, mockPrincipal);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(assetFacade).deleteAsset(mockAssetId, mockUserId);
	}
}
