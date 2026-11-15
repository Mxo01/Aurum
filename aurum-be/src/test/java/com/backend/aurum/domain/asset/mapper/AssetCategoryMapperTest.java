package com.backend.aurum.domain.asset.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.AssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.CreateAssetCategoryDTO;
import com.backend.aurum.domain.asset.dto.UpdateAssetCategoryDTO;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.AssetType;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AssetCategoryMapperTest {

	private static final String USER_ID_FIELD = "userId";
	private static final String USER_NOT_FOUND_MESSAGE = "User not found";

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private AssetCategoryMapper testSubject;

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void toEntity_rejectsANullUserId_beforeTheOwnerIsLoaded(CategoryBody body) {
		// GIVEN / WHEN / THEN
		assertThatThrownBy(() -> body.mapping().map(testSubject, null))
			.isInstanceOf(NullPointerException.class)
			.hasMessage(USER_ID_FIELD);
		verifyNoInteractions(userRepository);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void toEntity_mapsTheBodyAndResolvesTheOwner(CategoryBody body) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		AssetCategory result = body.mapping().map(testSubject, mockUserId);

		// THEN
		assertThat(result.getName()).isEqualTo(body.name());
		assertThat(result.getType()).isEqualTo(body.type());
		assertThat(result.getIcon()).isEqualTo(body.icon());
		assertThat(result.getUser()).isEqualTo(mockUser);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("bodies")
	void toEntity_throwsRuntimeException_whenTheOwnerDoesNotExist(CategoryBody body) {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> body.mapping().map(testSubject, mockUserId))
			.isInstanceOf(RuntimeException.class)
			.hasMessage(USER_NOT_FOUND_MESSAGE);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("absentBodies")
	void toEntity_returnsNull_whenTheBodyIsAbsent(String scenario, CategoryMapping mapping) {
		// GIVEN / WHEN
		AssetCategory result = mapping.map(testSubject, UUID.randomUUID());

		// THEN
		assertThat(result).isNull();
		verifyNoInteractions(userRepository);
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

	private static Stream<CategoryBody> bodies() {
		CreateAssetCategoryDTO mockCreate = Instancio.create(CreateAssetCategoryDTO.class);
		AssetCategoryDTO mockDetails = Instancio.create(AssetCategoryDTO.class);
		UpdateAssetCategoryDTO mockUpdate = Instancio.create(UpdateAssetCategoryDTO.class);

		return Stream.of(
			new CategoryBody(
				"the create body",
				mockCreate.getName(),
				mockCreate.getType(),
				mockCreate.getIcon(),
				(mapper, userId) -> mapper.toEntity(mockCreate, userId)
			),
			new CategoryBody(
				"the response body",
				mockDetails.getName(),
				mockDetails.getType(),
				mockDetails.getIcon(),
				(mapper, userId) -> mapper.toEntity(mockDetails, userId)
			),
			new CategoryBody(
				"the update body",
				mockUpdate.getName(),
				mockUpdate.getType(),
				mockUpdate.getIcon(),
				(mapper, userId) -> mapper.toEntity(mockUpdate, userId)
			)
		);
	}

	private static Stream<Arguments> absentBodies() {
		return Stream.of(
			Arguments.of(
				"the create body",
				(CategoryMapping) (mapper, userId) -> mapper.toEntity((CreateAssetCategoryDTO) null, userId)
			),
			Arguments.of(
				"the response body",
				(CategoryMapping) (mapper, userId) -> mapper.toEntity((AssetCategoryDTO) null, userId)
			),
			Arguments.of(
				"the update body",
				(CategoryMapping) (mapper, userId) -> mapper.toEntity((UpdateAssetCategoryDTO) null, userId)
			)
		);
	}

	private record CategoryBody(
		String scenario,
		String name,
		AssetType type,
		String icon,
		CategoryMapping mapping
	) {
		@Override
		public String toString() {
			return scenario;
		}
	}

	@FunctionalInterface
	private interface CategoryMapping {
		AssetCategory map(AssetCategoryMapper mapper, UUID userId);
	}
}
