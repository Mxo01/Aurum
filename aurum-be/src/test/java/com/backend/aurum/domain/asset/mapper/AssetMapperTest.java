package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
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
class AssetMapperTest {

	@Mock
	private AssetCategoryRepository categoryRepository;

	@Mock
	private UserRepository userRepository;

	@Mock
	private SnapshotMapper snapshotMapper;

	@InjectMocks
	private AssetMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Asset result = testSubject.toEntity((CreateAssetDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsResolvesUserAndCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.set(Select.field(CreateAssetDTO::getOriginalCurrency), Currency.EUR)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.EUR);
		assertThat(result.getUser()).isEqualTo(mockUser);
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toEntity_createDto_throwsRuntimeException_whenUserNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void toEntity_createDto_throwsRuntimeException_whenCategoryNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		CreateAssetDTO mockDto = Instancio.create(CreateAssetDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockDto.getCategoryId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void toEntity_createDto_throwsRuntimeException_whenCategoryBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockOtherUser)
			.create();
		CreateAssetDTO mockDto = Instancio.of(CreateAssetDTO.class)
			.set(Select.field(CreateAssetDTO::getCategoryId), mockCategory.getId())
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void toEntity_updateDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		Asset result = testSubject.toEntity((UpdateAssetDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_updateDto_mapsFieldsResolvesUserAndCategory() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		UpdateAssetDTO mockDto = Instancio.of(UpdateAssetDTO.class)
			.set(Select.field(UpdateAssetDTO::getCategoryId), mockCategory.getId())
			.set(Select.field(UpdateAssetDTO::getOriginalCurrency), Currency.USD)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		Asset result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getOriginalCurrency()).isEqualTo(Currency.USD);
		assertThat(result.getUser()).isEqualTo(mockUser);
		assertThat(result.getCategory()).isEqualTo(mockCategory);
	}

	@Test
	void toDto_mapsAllFieldsIncludingSnapshots() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		Snapshot mockSnapshot = Instancio.create(Snapshot.class);
		SnapshotDTO stubbedSnapshotDto = Instancio.create(SnapshotDTO.class);
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), mockCategory)
			.set(Select.field(Asset::getSnapshots), new ArrayList<>(List.of(mockSnapshot)))
			.create();
		when(snapshotMapper.toDto(mockSnapshot)).thenReturn(stubbedSnapshotDto);

		// WHEN
		AssetDTO result = testSubject.toDto(mockAsset);

		// THEN
		assertThat(result.getId()).isEqualTo(mockAsset.getId());
		assertThat(result.getName()).isEqualTo(mockAsset.getName());
		assertThat(result.getCategoryId()).isEqualTo(mockCategory.getId());
		assertThat(result.getSnapshots()).containsExactly(stubbedSnapshotDto);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN / WHEN
		AssetDTO result = testSubject.toDto(null);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDtoLight_populatesLatestAndPreviousValues_fromSnapshotList() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), mockCategory)
			.create();
		Snapshot mockLatestSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), new BigDecimal("500"))
			.set(Select.field(Snapshot::getExchangeRateToBase), BigDecimal.ONE)
			.create();
		Snapshot mockPreviousSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getAmountOriginalCurrency), new BigDecimal("400"))
			.create();
		List<Snapshot> mockLatestTwo = List.of(mockLatestSnapshot, mockPreviousSnapshot);

		// WHEN
		AssetDTO result = testSubject.toDtoLight(mockAsset, mockLatestTwo);

		// THEN
		assertThat(result.getLatestValue()).isEqualByComparingTo(new BigDecimal("500"));
		assertThat(result.getPreviousValue()).isEqualByComparingTo(new BigDecimal("400"));
	}

	@Test
	void toDtoLight_leavesLatestValueNull_whenSnapshotListIsEmpty() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.create();

		// WHEN
		AssetDTO result = testSubject.toDtoLight(mockAsset, List.of());

		// THEN
		assertThat(result.getLatestValue()).isNull();
		assertThat(result.getPreviousValue()).isNull();
	}
}
