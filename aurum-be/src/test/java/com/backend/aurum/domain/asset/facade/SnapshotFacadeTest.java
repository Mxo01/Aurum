package com.backend.aurum.domain.asset.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.mapper.SnapshotMapper;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.service.AssetService;
import com.backend.aurum.domain.asset.service.SnapshotService;
import com.backend.aurum.domain.asset.validation.SnapshotValidationService;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.backend.aurum.infrastructure.exchange.ExchangeRateService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SnapshotFacadeTest {

	@Mock
	private SnapshotService snapshotService;

	@Mock
	private SnapshotMapper mapper;

	@Mock
	private SnapshotValidationService validationService;

	@Mock
	private AssetService assetService;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private ExchangeRateService exchangeRateService;

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private SnapshotFacade testSubject;

	@Test
	void getSnapshots_returnsMappedDtos() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		List<SnapshotDTO> stubbedDtos = Instancio.ofList(SnapshotDTO.class).size(2).create();
		when(snapshotService.findByAssetId(mockAssetId, mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0))).thenReturn(stubbedDtos.get(0));
		when(mapper.toDto(stubbedSnapshots.get(1))).thenReturn(stubbedDtos.get(1));

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getSnapshots(mockAssetId, mockUserId);

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void getAllSnapshots_returnsMappedDtos() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<Snapshot> stubbedSnapshots = Instancio.ofList(Snapshot.class).size(2).create();
		List<SnapshotDTO> stubbedDtos = Instancio.ofList(SnapshotDTO.class).size(2).create();
		when(snapshotService.findAll(mockUserId)).thenReturn(stubbedSnapshots);
		when(mapper.toDto(stubbedSnapshots.get(0))).thenReturn(stubbedDtos.get(0));
		when(mapper.toDto(stubbedSnapshots.get(1))).thenReturn(stubbedDtos.get(1));

		// WHEN
		List<SnapshotDTO> expectedDtos = testSubject.getAllSnapshots(mockUserId);

		// THEN
		assertThat(expectedDtos).isEqualTo(stubbedDtos);
	}

	@Test
	void createSnapshot_usesExplicitExchangeRate_whenProvided() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		BigDecimal mockExplicitRate = new BigDecimal("1.25");
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), mockExplicitRate)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), BigDecimal.TEN)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getLiabilityType), null)
			.create();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Snapshot mockSaved = Instancio.create(Snapshot.class);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			snapshotService.saveOrUpdate(
				mockAsset,
				mockDto.getAmountOriginalCurrency(),
				mockDto.getReferenceDate(),
				mockExplicitRate
			)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
		verify(validationService).validate(mockDto, mockUserId);
	}

	@Test
	void createSnapshot_usesBigDecimalOne_whenCurrenciesMatch() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Currency sharedCurrency = Currency.EUR;
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), BigDecimal.TEN)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), sharedCurrency)
			.set(Select.field(Asset::getLiabilityType), null)
			.create();
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), mockUserId)
			.set(Select.field(User::getCurrency), sharedCurrency)
			.create();
		Snapshot mockSaved = Instancio.create(Snapshot.class);
		SnapshotDTO stubbedDto = Instancio.create(SnapshotDTO.class);
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			snapshotService.saveOrUpdate(
				mockAsset,
				mockDto.getAmountOriginalCurrency(),
				mockDto.getReferenceDate(),
				BigDecimal.ONE
			)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved)).thenReturn(stubbedDto);

		// WHEN
		SnapshotDTO expectedDto = testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		assertThat(expectedDto).isEqualTo(stubbedDto);
	}

	@Test
	void createSnapshot_deactivatesAsset_whenAmountIsZeroAndLiabilityTypeIsSet() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		Currency sharedCurrency = Currency.EUR;
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.set(Select.field(CreateSnapshotDTO::getAmountOriginalCurrency), BigDecimal.ZERO)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getOriginalCurrency), sharedCurrency)
			.set(Select.field(Asset::getLiabilityType), LiabilityType.MANUAL)
			.create();
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), mockUserId)
			.set(Select.field(User::getCurrency), sharedCurrency)
			.create();
		Snapshot mockSaved = Instancio.create(Snapshot.class);
		when(assetService.findById(mockDto.getAssetId(), mockUserId)).thenReturn(mockAsset);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(
			snapshotService.saveOrUpdate(
				mockAsset,
				BigDecimal.ZERO,
				mockDto.getReferenceDate(),
				BigDecimal.ONE
			)
		).thenReturn(mockSaved);
		when(mapper.toDto(mockSaved)).thenReturn(Instancio.create(SnapshotDTO.class));

		// WHEN
		testSubject.createSnapshot(mockDto, mockUserId);

		// THEN
		verify(assetRepository).save(mockAsset);
		assertThat(mockAsset.getIsActive()).isFalse();
	}

	@Test
	void deleteBulk_delegatesToService() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		UUID mockUserId = UUID.randomUUID();
		List<UUID> mockIds = List.of(UUID.randomUUID(), UUID.randomUUID());

		// WHEN
		testSubject.deleteBulk(mockAssetId, mockIds, mockUserId);

		// THEN
		verify(snapshotService).deleteBulk(mockIds, mockAssetId, mockUserId);
	}
}
