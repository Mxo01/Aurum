package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
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
class AssetCategoryMapperTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AssetCategoryMapper testSubject;

	@Test
	void toEntity_createDto_returnsNull_whenDtoIsNull() {
		// GIVEN / WHEN
		AssetCategory result = testSubject.toEntity((CreateAssetCategoryDTO) null, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toEntity_createDto_mapsFieldsAndResolvesUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		AssetCategory result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getType()).isEqualTo(mockDto.getType());
		assertThat(result.getIcon()).isEqualTo(mockDto.getIcon());
		assertThat(result.getUser()).isEqualTo(mockUser);
	}

	@Test
	void toEntity_createDto_throwsRuntimeException_whenUserNotFound() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		CreateAssetCategoryDTO mockDto = Instancio.create(CreateAssetCategoryDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.toEntity(mockDto, mockUserId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void toEntity_updateDto_mapsFieldsAndResolvesUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		UpdateAssetCategoryDTO mockDto = Instancio.create(UpdateAssetCategoryDTO.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		AssetCategory result = testSubject.toEntity(mockDto, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(mockDto.getName());
		assertThat(result.getType()).isEqualTo(mockDto.getType());
		assertThat(result.getUser()).isEqualTo(mockUser);
	}

	@Test
	void toDto_returnsNull_whenEntityIsNull() {
		// GIVEN / WHEN
		AssetCategoryDTO result = testSubject.toDto(null);

		// THEN
		assertThat(result).isNull();
	}

	@Test
	void toDto_mapsAllFields() {
		// GIVEN
		User mockUser = Instancio.create(User.class);
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();

		// WHEN
		AssetCategoryDTO result = testSubject.toDto(mockCategory);

		// THEN
		assertThat(result.getId()).isEqualTo(mockCategory.getId());
		assertThat(result.getName()).isEqualTo(mockCategory.getName());
		assertThat(result.getType()).isEqualTo(mockCategory.getType());
		assertThat(result.getIcon()).isEqualTo(mockCategory.getIcon());
		assertThat(result.isDefault()).isFalse();
	}

	@Test
	void toDto_marksAsDefault_whenUserIsNull() {
		// GIVEN
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();

		// WHEN
		AssetCategoryDTO result = testSubject.toDto(mockCategory);

		// THEN
		assertThat(result.isDefault()).isTrue();
	}
}
