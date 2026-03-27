package com.backend.aurum.domain.asset.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.AssetStatusDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.mapper.AssetMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.validation.AssetValidationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetFacadeTest {

	@Mock
	private AssetService assetService;

	@Mock
	private AssetMapper mapper;

	@Mock
	private AssetValidationService validationService;

	@Mock
	private SnapshotRepository snapshotRepository;

	@InjectMocks
	private AssetFacade testSubject;

	@Test
	void getAsset_returnsMappedDto() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		Asset mockAsset = Instancio.create(Asset.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(assetService.findById(mockAssetId, mockUserId)).thenReturn(mockAsset);
		when(mapper.toDto(mockAsset)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.getAsset(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void createAsset_validatesAndReturnsDto() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		Asset mockAsset = Instancio.create(Asset.class);
		Asset mockSaved = Instancio.create(Asset.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockAsset);
		when(
			assetService.save(mockAsset, mockDto.getInitialValue(), mockDto.getReferenceDate())
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.createAsset(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateAsset_validatesAndReturnsDto() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		Asset mockDetails = Instancio.create(Asset.class);
		Asset mockUpdated = Instancio.create(Asset.class);
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockDetails);
		when(assetService.update(mockId, mockDetails, mockUserId)).thenReturn(mockUpdated);
		when(mapper.toDto(mockUpdated)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.updateAsset(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void updateAssetLight_validatesAndReturnsLightDto() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		UpdateAssetDTO mockDto = Instancio.create(UpdateAssetDTO.class);
		Asset mockDetails = Instancio.create(Asset.class);
		Asset mockUpdated = Instancio.create(Asset.class);
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(mapper.toEntity(mockDto, mockUserId)).thenReturn(mockDetails);
		when(assetService.update(mockId, mockDetails, mockUserId)).thenReturn(mockUpdated);
		when(snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(mockId)).thenReturn(
			stubbedSnapshots
		);
		when(mapper.toDtoLight(mockUpdated, stubbedSnapshots)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.updateAssetLight(mockId, mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto);
	}

	@Test
	void patchAssetStatus_throwsIllegalArgument_whenIsActiveIsNull() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		AssetStatusDTO mockStatusDto = Instancio.of(AssetStatusDTO.class)
			.set(Select.field(AssetStatusDTO::getIsActive), null)
			.create();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.patchAssetStatus(mockId, mockStatusDto, mockUserId)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void patchAssetStatus_returnsLightDto_whenIsActiveIsProvided() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		AssetStatusDTO mockStatusDto = Instancio.of(AssetStatusDTO.class)
			.set(Select.field(AssetStatusDTO::getIsActive), true)
			.set(Select.field(AssetStatusDTO::getChangedAt), LocalDate.now())
			.create();
		Asset mockUpdated = Instancio.create(Asset.class);
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		AssetDTO stubbedDto = Instancio.create(AssetDTO.class);
		when(
			assetService.patchStatus(mockId, true, mockStatusDto.getChangedAt(), mockUserId)
		).thenReturn(mockUpdated);
		when(snapshotRepository.findTop2ByAssetIdOrderByReferenceDateDesc(mockId)).thenReturn(
			stubbedSnapshots
		);
		when(mapper.toDtoLight(mockUpdated, stubbedSnapshots)).thenReturn(stubbedDto);

		// WHEN
		AssetDTO expectedDto = testSubject.patchAssetStatus(mockId, mockStatusDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void deleteAsset_delegatesToService() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();

		// WHEN
		testSubject.deleteAsset(mockId, mockUserId);

		// THEN
		verify(assetService).delete(mockId, mockUserId);
	}

	@Test
	void getAllAssets_returnsMappedLightDtos() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Asset> stubbedAssets = Instancio.ofList(Asset.class).size(2).create();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(0).create();
		AssetDTO stubbedDto1 = Instancio.create(AssetDTO.class);
		AssetDTO stubbedDto2 = Instancio.create(AssetDTO.class);
		when(assetService.findAll(mockUserId)).thenReturn(stubbedAssets);
		when(snapshotRepository.findByAssetUserIdOrderByReferenceDateDesc(mockUserId)).thenReturn(
			stubbedSnapshots
		);
		when(mapper.toDtoLight(stubbedAssets.get(0), Map.of())).thenReturn(stubbedDto1);
		when(mapper.toDtoLight(stubbedAssets.get(1), Map.of())).thenReturn(stubbedDto2);

		// WHEN
		List<AssetDTO> expectedDtos = testSubject.getAllAssets(mockUserId);

		// THEN
		assertThat(expectedDtos).containsExactly(stubbedDto1, stubbedDto2);
	}
}
