package com.backend.aurum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.UsersEntity;
import com.auth0.exception.Auth0Exception;
import com.auth0.net.Request;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.repository.UserRepository;
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
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private Auth0ManagementService auth0ManagementService;

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private AssetCategoryRepository categoryRepository;

	@Mock
	private TargetRepository targetRepository;

	@InjectMocks
	private UserService testSubject;

	@Test
	void findOrCreate_returnsExistingUser_whenFound() {
		// GIVEN
		String mockJwtId = "auth0|existing";
		User mockUser = Instancio.create(User.class);
		when(userRepository.findByJwtId(mockJwtId)).thenReturn(Optional.of(mockUser));

		// WHEN
		User expectedUser = testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(expectedUser).isEqualTo(mockUser);
	}

	@Test
	void findOrCreate_createsNewUser_whenNotFound() {
		// GIVEN
		String mockJwtId = "auth0|newuser";
		User stubbedCreated = Instancio.create(User.class);
		when(userRepository.findByJwtId(mockJwtId)).thenReturn(Optional.empty());
		when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.notNull())).thenReturn(
			stubbedCreated
		);

		// WHEN
		User expectedUser = testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(expectedUser).isEqualTo(stubbedCreated);
	}

	@Test
	void findOrCreate_retriesLookup_whenConcurrentInsertOccurs() {
		// GIVEN
		String mockJwtId = "auth0|concurrent";
		User stubbedUser = Instancio.create(User.class);
		when(userRepository.findByJwtId(mockJwtId))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(stubbedUser));
		when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.notNull())).thenThrow(
			new RuntimeException("duplicate key")
		);

		// WHEN
		User expectedUser = testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(expectedUser).isEqualTo(stubbedUser);
	}

	@Test
	void getUserById_returnsUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		User expectedUser = testSubject.getUserById(mockUserId);

		// THEN
		assertThat(expectedUser).isEqualTo(mockUser);
	}

	@Test
	void deleteUser_deletesFromRepositoryAndAuth0() throws Auth0Exception {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|delete";
		ManagementAPI mockMgmt = org.mockito.Mockito.mock(ManagementAPI.class);
		UsersEntity mockUsers = org.mockito.Mockito.mock(UsersEntity.class);
		Request<Void> mockRequest = org.mockito.Mockito.mock(Request.class);
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(mockUsers.delete(mockJwtId)).thenReturn(mockRequest);

		// WHEN
		testSubject.deleteUser(mockUserId, mockJwtId);

		// THEN
		verify(userRepository).deleteById(mockUserId);
		verify(mockRequest).execute();
	}

	@Test
	void deleteUser_throwsRuntimeException_whenAuth0Fails() throws Auth0Exception {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|faildelete";
		ManagementAPI mockMgmt = org.mockito.Mockito.mock(ManagementAPI.class);
		UsersEntity mockUsers = org.mockito.Mockito.mock(UsersEntity.class);
		Request<Void> mockRequest = org.mockito.Mockito.mock(Request.class);
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(mockUsers.delete(mockJwtId)).thenReturn(mockRequest);
		when(mockRequest.execute()).thenThrow(new Auth0Exception("Auth0 error"));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteUser(mockUserId, mockJwtId)).isInstanceOf(
			RuntimeException.class
		);
	}

	@Test
	void updateCurrency_savesCurrencyToUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		UpdateCurrencyDTO mockDto = new UpdateCurrencyDTO(Currency.USD);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updateCurrency(mockUserId, mockDto);

		// THEN
		assertThat(mockUser.getCurrency()).isEqualTo(Currency.USD);
		verify(userRepository).save(mockUser);
	}

	@Test
	void updateLocale_savesLocaleToUser() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.create(User.class);
		UpdateLocaleDTO mockDto = new UpdateLocaleDTO(Locale.EN_US);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updateLocale(mockUserId, mockDto);

		// THEN
		assertThat(mockUser.getLocale()).isEqualTo(Locale.EN_US);
		verify(userRepository).save(mockUser);
	}

	@Test
	void deletePicture_setsUserPictureToNull() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getPicture), "data:image/jpeg;base64,abc")
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.deletePicture(mockUserId);

		// THEN
		assertThat(mockUser.getPicture()).isNull();
		verify(userRepository).save(mockUser);
	}

	@Test
	void exportUserData_returnsExportWithUserProfile() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockEmail = "test@example.com";
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getId), mockUserId)
			.set(Select.field(User::getCurrency), Currency.EUR)
			.set(Select.field(User::getLocale), Locale.EN_US)
			.create();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findByUserId(mockUserId)).thenReturn(List.of());
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(List.of());
		when(targetRepository.findByUserId(mockUserId)).thenReturn(List.of());

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUserId, mockEmail);

		// THEN
		assertThat(expectedExport).isNotNull();
		assertThat(expectedExport.profile().id()).isEqualTo(mockUserId);
		assertThat(expectedExport.profile().email()).isEqualTo(mockEmail);
	}

	@Test
	void isGoogleUser_returnsTrue_whenJwtIdStartsWithGooglePrefix() {
		// GIVEN
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getJwtId), "google-oauth2|123456")
			.create();
		Jwt mockJwt = org.mockito.Mockito.mock(Jwt.class);
		UserPrincipal mockPrincipal = new UserPrincipal(mockUser, mockJwt);

		// WHEN
		boolean expectedResult = testSubject.isGoogleUser(mockPrincipal);

		// THEN
		assertThat(expectedResult).isTrue();
	}

	@Test
	void isGoogleUser_returnsFalse_whenJwtIdIsAuth0() {
		// GIVEN
		User mockUser = Instancio.of(User.class)
			.set(Select.field(User::getJwtId), "auth0|123456")
			.create();
		Jwt mockJwt = org.mockito.Mockito.mock(Jwt.class);
		UserPrincipal mockPrincipal = new UserPrincipal(mockUser, mockJwt);

		// WHEN
		boolean expectedResult = testSubject.isGoogleUser(mockPrincipal);

		// THEN
		assertThat(expectedResult).isFalse();
	}

	@Test
	void updateAuth0Name_throwsRuntimeException_whenAuth0Fails() throws Auth0Exception {
		// GIVEN
		String mockJwtId = "auth0|user";
		UpdateNameDTO mockDto = new UpdateNameDTO("New Name");
		ManagementAPI mockMgmt = org.mockito.Mockito.mock(ManagementAPI.class);
		UsersEntity mockUsers = org.mockito.Mockito.mock(UsersEntity.class);
		Request<com.auth0.json.mgmt.users.User> mockRequest = org.mockito.Mockito.mock(Request.class);
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(
			mockUsers.update(
				org.mockito.ArgumentMatchers.eq(mockJwtId),
				org.mockito.ArgumentMatchers.notNull()
			)
		).thenReturn(mockRequest);
		when(mockRequest.execute()).thenThrow(new Auth0Exception("update failed"));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.updateAuth0Name(mockJwtId, mockDto)).isInstanceOf(
			RuntimeException.class
		);
	}
}
