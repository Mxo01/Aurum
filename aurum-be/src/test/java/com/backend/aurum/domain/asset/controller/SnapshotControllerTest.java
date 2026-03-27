package com.backend.aurum.domain.asset.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.facade.SnapshotFacade;
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
class SnapshotControllerTest {

	@Mock
	private SnapshotFacade snapshotFacade;

	@InjectMocks
	private SnapshotController testSubject;

	private UserPrincipal buildPrincipal(UUID userId) {
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), userId).create();
		return new UserPrincipal(mockUser, mock(Jwt.class));
	}

	@Test
	void getSnapshots_withAssetId_returnsSnapshotsForAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<SnapshotDTO> stubbedDtos = Instancio.ofList(SnapshotDTO.class).size(2).create();
		when(snapshotFacade.getSnapshots(mockAssetId, mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<SnapshotDTO>> expectedResponse = testSubject.getSnapshots(
			mockPrincipal,
			mockAssetId
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void getSnapshots_withoutAssetId_returnsAllSnapshots() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<SnapshotDTO> stubbedDtos = Instancio.ofList(SnapshotDTO.class).size(3).create();
		when(snapshotFacade.getAllSnapshots(mockUserId)).thenReturn(stubbedDtos);

		// WHEN
		ResponseEntity<List<SnapshotDTO>> expectedResponse = testSubject.getSnapshots(
			mockPrincipal,
			null
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDtos);
	}

	@Test
	void createSnapshot_returnsOkWithDtoFromFacade() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		when(snapshotFacade.createSnapshot(mockDto, mockUserId)).thenReturn(stubbedDto);

		// WHEN
		ResponseEntity<SnapshotDTO> expectedResponse = testSubject.createSnapshot(
			mockDto,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(expectedResponse.getBody()).isEqualTo(stubbedDto);
	}

	@Test
	void deleteSnapshotsBulk_returnsNoContent() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID mockAssetId = UUID.randomUUID();
		UserPrincipal mockPrincipal = buildPrincipal(mockUserId);
		List<UUID> mockIds = List.of(UUID.randomUUID(), UUID.randomUUID());

		// WHEN
		ResponseEntity<Void> expectedResponse = testSubject.deleteSnapshotsBulk(
			mockAssetId,
			mockIds,
			mockPrincipal
		);

		// THEN
		assertThat(expectedResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		verify(snapshotFacade).deleteBulk(mockAssetId, mockIds, mockUserId);
	}
}
