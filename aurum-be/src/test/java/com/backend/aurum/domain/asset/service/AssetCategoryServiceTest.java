package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.user.model.User;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AssetCategoryServiceTest {

	private static final long NO_ASSETS = 0L;
	private static final long ASSET_COUNT_IN_CATEGORY = 2L;
	private static final String DEFAULT_UPDATE_REASON = "A default category cannot be changed";
	private static final String FOREIGN_UPDATE_REASON = "You can only change your own categories";
	private static final String DEFAULT_DELETE_REASON = "A default category cannot be deleted";
	private static final String FOREIGN_DELETE_REASON = "You can only delete your own categories";
	private static final String CATEGORY_IN_USE_REASON =
		"This category still has assets, move or delete them first";

	@Mock
	private AssetCategoryRepository categoryRepository;

	@Mock
	private AssetRepository assetRepository;

	@InjectMocks
	private AssetCategoryService testSubject;

	@Test
	void findAll_returnsCategoriesFromRepository() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		List<AssetCategory> stubbedCategories = Instancio.ofList(AssetCategory.class).size(3).create();
		when(categoryRepository.findByUserIdOrUserIsNull(mockUserId)).thenReturn(stubbedCategories);

		// WHEN
		List<AssetCategory> expectedCategories = testSubject.findAll(mockUserId);

		// THEN
		assertThat(expectedCategories).isEqualTo(stubbedCategories);
	}

	@Test
	void findById_returnsCategory_whenFound() {
		// GIVEN
		AssetCategory mockCategory = Instancio.create(AssetCategory.class);
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN
		AssetCategory expectedCategory = testSubject.findById(mockCategory.getId());

		// THEN
		assertThat(expectedCategory).isEqualTo(mockCategory);
	}

	@Test
	void findById_throwsRuntimeException_whenNotFound() {
		// GIVEN
		UUID mockId = UUID.randomUUID();
		when(categoryRepository.findById(mockId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findById(mockId)).isInstanceOf(RuntimeException.class);
	}

	@Test
	void save_returnsSavedCategory() {
		// GIVEN
		AssetCategory mockCategory = Instancio.create(AssetCategory.class);
		AssetCategory stubbedSaved = Instancio.create(AssetCategory.class);
		when(categoryRepository.save(mockCategory)).thenReturn(stubbedSaved);

		// WHEN
		AssetCategory expectedCategory = testSubject.save(mockCategory);

		// THEN
		assertThat(expectedCategory).isEqualTo(stubbedSaved);
	}

	@Test
	void update_returnsUpdatedCategory_whenAuthorized() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		AssetCategory mockDetails = Instancio.create(AssetCategory.class);
		AssetCategory stubbedUpdated = Instancio.create(AssetCategory.class);
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));
		when(categoryRepository.save(mockCategory)).thenReturn(stubbedUpdated);

		// WHEN
		AssetCategory expectedCategory = testSubject.update(
			mockCategory.getId(),
			mockDetails,
			mockUserId
		);

		// THEN
		assertThat(expectedCategory).isEqualTo(stubbedUpdated);
	}

	@Test
	void update_throwsForbidden_whenCategoryIsDefault() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		AssetCategory mockDetails = Instancio.create(AssetCategory.class);
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.update(mockCategory.getId(), mockDetails, mockUserId)
		).isInstanceOfSatisfying(ResponseStatusException.class, expectedException -> {
			assertThat(expectedException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(expectedException.getReason()).isEqualTo(DEFAULT_UPDATE_REASON);
		});
	}

	@Test
	void update_throwsForbidden_whenCategoryBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockOtherUser)
			.create();
		AssetCategory mockDetails = Instancio.create(AssetCategory.class);
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.update(mockCategory.getId(), mockDetails, mockUserId)
		).isInstanceOfSatisfying(ResponseStatusException.class, expectedException -> {
			assertThat(expectedException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(expectedException.getReason()).isEqualTo(FOREIGN_UPDATE_REASON);
		});
	}

	@Test
	void delete_delegatesToRepository_whenOwnCategoryHoldsNoAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));
		when(assetRepository.countByCategoryId(mockCategory.getId())).thenReturn(NO_ASSETS);

		// WHEN
		testSubject.delete(mockCategory.getId(), mockUserId);

		// THEN
		verify(categoryRepository).deleteById(mockCategory.getId());
	}

	@Test
	void delete_throwsConflict_whenCategoryStillHoldsAssets() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getId), mockUserId).create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockUser)
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));
		when(assetRepository.countByCategoryId(mockCategory.getId())).thenReturn(
			ASSET_COUNT_IN_CATEGORY
		);

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.delete(mockCategory.getId(), mockUserId)
		).isInstanceOfSatisfying(ResponseStatusException.class, expectedException -> {
			assertThat(expectedException.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
			assertThat(expectedException.getReason()).isEqualTo(CATEGORY_IN_USE_REASON);
		});
		verify(categoryRepository, never()).deleteById(mockCategory.getId());
	}

	@Test
	void delete_throwsForbidden_whenCategoryIsDefault() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), null)
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.delete(mockCategory.getId(), mockUserId)
		).isInstanceOfSatisfying(ResponseStatusException.class, expectedException -> {
			assertThat(expectedException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(expectedException.getReason()).isEqualTo(DEFAULT_DELETE_REASON);
		});
		verifyNoInteractions(assetRepository);
	}

	@Test
	void delete_throwsForbidden_whenCategoryBelongsToDifferentUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UUID otherUserId = UUID.randomUUID();
		User mockOtherUser = Instancio.of(User.class)
			.set(Select.field(User::getId), otherUserId)
			.create();
		AssetCategory mockCategory = Instancio.of(AssetCategory.class)
			.set(Select.field(AssetCategory::getUser), mockOtherUser)
			.create();
		when(categoryRepository.findById(mockCategory.getId())).thenReturn(Optional.of(mockCategory));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.delete(mockCategory.getId(), mockUserId)
		).isInstanceOfSatisfying(ResponseStatusException.class, expectedException -> {
			assertThat(expectedException.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
			assertThat(expectedException.getReason()).isEqualTo(FOREIGN_DELETE_REASON);
		});
		verifyNoInteractions(assetRepository);
	}
}
