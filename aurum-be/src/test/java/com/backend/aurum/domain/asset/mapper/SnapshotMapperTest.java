package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.infrastructure.exception.AccessDeniedException;
import com.backend.aurum.infrastructure.exception.NotFoundException;
import java.math.BigDecimal;
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
class SnapshotMapperTest {

	@Mock
	private AssetRepository assetRepository;

	@InjectMocks
	private SnapshotMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Snapshot result = testSubject.toEntity((CreateSnapshotDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsAndResolvesAsset() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), new BigDecimal("1.5"))
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getReferenceDate()).isEqualTo(mockDto.getReferenceDate());
		assertThat(result.getAmountOriginalCurrency()).isEqualTo(mockDto.getAmountOriginalCurrency());
		assertThat(result.getExchangeRateToBase()).isEqualTo(mockDto.getExchangeRateToBase());
		assertThat(result.getAsset()).isEqualTo(mockAsset);
	}

	@Test
	void toEntity_createDto_defaultsExchangeRateToOne_whenNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.set(Select.field(CreateSnapshotDTO::getExchangeRateToBase), null)
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN
		Snapshot result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getExchangeRateToBase()).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void toEntity_createDto_throwsNotFoundException_whenAssetNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateSnapshotDTO mockDto = Instancio.create(CreateSnapshotDTO.class);
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void toEntity_createDto_throwsAccessDeniedException_whenAssetBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getUser), mockOtherUser)
			.create();
		CreateSnapshotDTO mockDto = Instancio.of(CreateSnapshotDTO.class)
			.set(Select.field(CreateSnapshotDTO::getAssetId), mockAsset.getId())
			.create();
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			AccessDeniedException.class
		);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN / WHEN
		SnapshotDTO result = testSubject.toDto(null);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_mapsAllFields() {
		// GIVEN
		Asset mockAsset = Instancio.create(Asset.class);
		Snapshot mockSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAsset), mockAsset)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), new BigDecimal("100"))
			.set(Select.field(Snapshot::getExchangeRateToBase), new BigDecimal("1.5"))
			.create();

		// WHEN
		SnapshotDTO result = testSubject.toDto(mockSnapshot);

		// THEN
		assertThat(result.getId()).isEqualTo(mockSnapshot.getId());
		assertThat(result.getReferenceDate()).isEqualTo(mockSnapshot.getReferenceDate());
		assertThat(result.getAmountOriginalCurrency()).isEqualTo(
			mockSnapshot.getAmountOriginalCurrency()
		);
		assertThat(result.getExchangeRateToBase()).isEqualTo(mockSnapshot.getExchangeRateToBase());
		assertThat(result.getAssetId()).isEqualTo(mockAsset.getId());
		assertThat(result.getAssetName()).isEqualTo(mockAsset.getName());
		assertThat(result.getAmountBaseCurrency()).isEqualByComparingTo(new BigDecimal("150.0000"));
	}
}
