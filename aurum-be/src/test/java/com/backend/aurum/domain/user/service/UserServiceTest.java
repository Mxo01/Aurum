package com.backend.aurum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.UsersEntity;
import com.auth0.exception.Auth0Exception;
import com.auth0.net.Request;
import com.backend.aurum.domain.analytics.model.Target;
import com.backend.aurum.domain.analytics.repository.TargetRepository;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.AssetCategory;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetCategoryRepository;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.auth.service.Auth0ManagementService;
import com.backend.aurum.domain.cashflow.model.CashFlow;
import com.backend.aurum.domain.cashflow.repository.CashFlowRepository;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.exception.ValidationException;
import com.backend.aurum.domain.user.dto.UpdateCurrencyDTO;
import com.backend.aurum.domain.user.dto.UpdateLocaleDTO;
import com.backend.aurum.domain.user.dto.UpdateNameDTO;
import com.backend.aurum.domain.user.dto.UserExportDTO;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.enums.Locale;
import com.backend.aurum.domain.user.model.User;
import com.backend.aurum.domain.user.model.UserPrincipal;
import com.backend.aurum.domain.user.repository.UserRepository;
import com.backend.aurum.domain.user.service.ProfilePictureFixture.Corner;
import com.backend.aurum.domain.user.service.ProfilePictureFixture.Format;
import com.backend.aurum.domain.user.service.ProfilePictureFixture.Quadrant;
import com.backend.aurum.domain.user.service.ProfilePictureFixture.QuadrantLayout;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.CRC32;
import javax.imageio.ImageIO;
import org.instancio.Instancio;
import org.instancio.Select;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final String NAME_FIELD = "name";

	private static final String FILE_FIELD = "file";

	private static final String DEFAULT_FILE_NAME = "avatar";

	private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

	private static final String MISSING_NAME_MESSAGE = "Name is required";

	private static final String NAME_TOO_LONG_MESSAGE = "Name cannot exceed 100 characters";

	private static final String NOT_AN_IMAGE_MESSAGE =
		"The picture must be a JPEG, PNG, GIF, BMP, or WebP image";

	private static final String PICTURE_TOO_LARGE_MESSAGE =
		"The picture is too large, choose a smaller one";

	private static final int MAX_NAME_LENGTH = 100;

	private static final String BLANK_NAME = "   ";

	private static final String VALID_NAME = "Ada Lovelace";

	private static final byte[] PNG_SIGNATURE = {
		(byte) 0x89,
		0x50,
		0x4E,
		0x47,
		0x0D,
		0x0A,
		0x1A,
		0x0A
	};

	private static final int PNG_MAGIC_BYTES = 4;

	private static final int TRUNCATED_PNG_LENGTH = 64;

	private static final int OVERSIZED_DIMENSION = 10_000;

	private static final int PROFILE_PICTURE_SIZE = 200;

	private static final int SOURCE_PICTURE_WIDTH = 300;

	private static final int SOURCE_PICTURE_HEIGHT = 200;

	private static final int PNG_BIT_DEPTH = 8;

	private static final int PNG_COLOR_TYPE_RGBA = 6;

	private static final int PNG_COMPRESSION_DEFLATE = 0;

	private static final int PNG_FILTER_ADAPTIVE = 0;

	private static final int PNG_INTERLACE_NONE = 0;

	private static final int TINY_PICTURE_WIDTH = 64;

	private static final int TINY_PICTURE_HEIGHT = 48;

	private static final String GIF_FORMAT = "gif";

	private static final String BMP_FORMAT = "bmp";

	private static final int WEBP_HEADER_LENGTH = 32;

	private static final byte[] WEBP_RIFF_HEADER = {
		'R',
		'I',
		'F',
		'F',
		0,
		0,
		0,
		0,
		'W',
		'E',
		'B',
		'P'
	};

	private static final int ORIENTATION_UNKNOWN = 9;

	private static final byte[] EXIF_PREFIX = { 'E', 'x', 'i', 'f', 0, 0 };

	private static final byte[] TIFF_LITTLE_ENDIAN_HEADER = { 'I', 'I', 42, 0, 8, 0, 0, 0 };

	private static final byte[] EXIF_SEGMENT_MARKER = { (byte) 0xFF, (byte) 0xE1 };

	private static final byte[] NO_NEXT_IFD = { 0, 0, 0, 0 };

	private static final int EXIF_ORIENTATION_TAG = 0x0112;

	private static final int EXIF_FOREIGN_TAG = 0x010F;

	private static final int TIFF_TYPE_ASCII = 2;

	private static final int TIFF_TYPE_SHORT = 3;

	private static final int ONE_EXIF_ENTRY = 1;

	private static final int NO_EXIF_ENTRY = 0;

	private static final int EXIF_VALUE_LENGTH = 4;

	private static final int EXIF_SEGMENT_LENGTH_BYTES = 2;

	private static final byte[] JPEG_MAGIC_BYTES = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };

	private static final String CURRENCY_FIELD = "currency";

	private static final String LOCALE_FIELD = "locale";

	private static final String MISSING_CURRENCY_MESSAGE = "Select a currency";

	private static final String MISSING_LOCALE_MESSAGE = "Select a language";

	private static final String UNPROCESSABLE_PICTURE_MESSAGE = "The picture could not be processed";

	private static final String FORGED_LINE_BREAK = "\n";

	private static final String FORGED_FILE_NAME = "avatar" + FORGED_LINE_BREAK + "ERROR forged";

	private static final String SANITIZED_FILE_NAME = "avatarERROR forged";

	private static final Level VISIBLE_LOG_LEVEL = Level.INFO;

	private static final Level DETAIL_LOG_LEVEL = Level.DEBUG;

	private enum ExifOrientation {
		IDENTITY(1, QuadrantLayout.SOURCE),
		MIRRORED_HORIZONTALLY(
			2,
			new QuadrantLayout(Quadrant.GREEN, Quadrant.RED, Quadrant.WHITE, Quadrant.BLUE)
		),
		ROTATED_180(3, new QuadrantLayout(Quadrant.WHITE, Quadrant.BLUE, Quadrant.GREEN, Quadrant.RED)),
		MIRRORED_VERTICALLY(
			4,
			new QuadrantLayout(Quadrant.BLUE, Quadrant.WHITE, Quadrant.RED, Quadrant.GREEN)
		),
		TRANSPOSED(5, new QuadrantLayout(Quadrant.RED, Quadrant.BLUE, Quadrant.GREEN, Quadrant.WHITE)),
		ROTATED_90_CLOCKWISE(
			6,
			new QuadrantLayout(Quadrant.BLUE, Quadrant.RED, Quadrant.WHITE, Quadrant.GREEN)
		),
		TRANSVERSE(7, new QuadrantLayout(Quadrant.WHITE, Quadrant.GREEN, Quadrant.BLUE, Quadrant.RED)),
		ROTATED_90_COUNTER_CLOCKWISE(
			8,
			new QuadrantLayout(Quadrant.GREEN, Quadrant.WHITE, Quadrant.RED, Quadrant.BLUE)
		);

		private final int value;
		private final QuadrantLayout layout;

		ExifOrientation(int value, QuadrantLayout layout) {
			this.value = value;
			this.layout = layout;
		}

		int value() {
			return value;
		}

		QuadrantLayout layout() {
			return layout;
		}
	}

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

	@Mock
	private CashFlowRepository cashFlowRepository;

	@InjectMocks
	private UserService testSubject;

	private ListAppender<ILoggingEvent> capturedLogLines;

	private Logger userServiceLogger;

	private Level stubbedLogLevel;

	@BeforeEach
	void captureLogLines() {
		userServiceLogger = (Logger) LoggerFactory.getLogger(UserService.class);
		stubbedLogLevel = userServiceLogger.getLevel();
		userServiceLogger.setLevel(DETAIL_LOG_LEVEL);
		capturedLogLines = new ListAppender<>();
		capturedLogLines.start();
		userServiceLogger.addAppender(capturedLogLines);
	}

	@AfterEach
	void releaseLogLines() {
		userServiceLogger.detachAppender(capturedLogLines);
		userServiceLogger.setLevel(stubbedLogLevel);
	}

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
			new DataIntegrityViolationException("duplicate key")
		);

		// WHEN
		User expectedUser = testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(expectedUser).isEqualTo(stubbedUser);
	}

	@Test
	void findOrCreate_throwsRuntimeException_whenTheRetryFindsNoUser() {
		// GIVEN
		String mockJwtId = "auth0|orphaned";
		when(userRepository.findByJwtId(mockJwtId)).thenReturn(Optional.empty());
		when(userRepository.saveAndFlush(org.mockito.ArgumentMatchers.notNull())).thenThrow(
			new DataIntegrityViolationException("duplicate key")
		);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.findOrCreate(mockJwtId)).isInstanceOf(
			RuntimeException.class
		);
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
	void getUserById_throwsNotFoundException_whenTheUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.getUserById(mockUserId)).isInstanceOf(
			NotFoundException.class
		);
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
	void updateCurrency_throwsNotFoundException_whenTheUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateCurrencyDTO mockDto = new UpdateCurrencyDTO(Currency.USD);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.updateCurrency(mockUserId, mockDto)).isInstanceOf(
			NotFoundException.class
		);
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
	void updateLocale_throwsNotFoundException_whenTheUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		UpdateLocaleDTO mockDto = new UpdateLocaleDTO(Locale.EN_US);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.updateLocale(mockUserId, mockDto)).isInstanceOf(
			NotFoundException.class
		);
	}

	@Test
	void updateCurrency_answersThePinned400_whenTheCurrencyIsMissing() {
		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateCurrency(UUID.randomUUID(), new UpdateCurrencyDTO(null))
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(CURRENCY_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(MISSING_CURRENCY_MESSAGE);
		});
	}

	@Test
	void updateLocale_answersThePinned400_whenTheLocaleIsMissing() {
		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateLocale(UUID.randomUUID(), new UpdateLocaleDTO(null))
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(LOCALE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(MISSING_LOCALE_MESSAGE);
		});
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
	void deletePicture_throwsNotFoundException_whenTheUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deletePicture(mockUserId)).isInstanceOf(
			NotFoundException.class
		);
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
		when(cashFlowRepository.findByUserIdOrderByYearDescMonthAsc(mockUserId)).thenReturn(List.of());

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUserId, mockEmail);

		// THEN
		assertThat(expectedExport).isNotNull();
		assertThat(expectedExport.profile().id()).isEqualTo(mockUserId);
		assertThat(expectedExport.profile().email()).isEqualTo(mockEmail);
	}

	@Test
	void exportUserData_carriesTheSnapshotSource() {
		// GIVEN
		Snapshot mockUserSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getId), UUID.randomUUID())
			.set(Select.field(Snapshot::getSource), SnapshotSource.USER)
			.create();
		Snapshot mockScheduledSnapshot = Instancio.of(Snapshot.class)
			.set(Select.field(Snapshot::getId), UUID.randomUUID())
			.set(Select.field(Snapshot::getSource), SnapshotSource.SCHEDULER)
			.create();
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getSnapshots), List.of(mockUserSnapshot, mockScheduledSnapshot))
			.create();
		User mockUser = stubExportDataSources(List.of(), List.of(mockAsset), List.of(), List.of());

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUser.getId(), "test@example.com");

		// THEN
		assertThat(expectedExport.assets())
			.singleElement()
			.satisfies(exportedAsset ->
				assertThat(exportedAsset.snapshots())
					.extracting(UserExportDTO.SnapshotExport::source)
					.containsExactly(SnapshotSource.USER.name(), SnapshotSource.SCHEDULER.name())
			);
	}

	@Test
	void exportUserData_mapsAssetCategoryAndLiabilityFields() {
		// GIVEN
		AssetCategory mockCategory = Instancio.create(AssetCategory.class);
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), mockCategory)
			.set(Select.field(Asset::getLiabilityType), LiabilityType.MANUAL)
			.set(Select.field(Asset::getPaymentFrequency), PaymentFrequency.MONTHLY)
			.create();
		User mockUser = stubExportDataSources(List.of(), List.of(mockAsset), List.of(), List.of());

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUser.getId(), "test@example.com");

		// THEN
		assertThat(expectedExport.assets())
			.singleElement()
			.satisfies(exportedAsset -> {
				assertThat(exportedAsset.category()).isEqualTo(mockCategory.getName());
				assertThat(exportedAsset.liabilityType()).isEqualTo(LiabilityType.MANUAL.name());
				assertThat(exportedAsset.paymentFrequency()).isEqualTo(PaymentFrequency.MONTHLY.name());
				assertThat(exportedAsset.paymentAmount()).isEqualByComparingTo(
					mockAsset.getPaymentAmount()
				);
			});
	}

	@Test
	void exportUserData_keepsAssetFieldsEmpty_whenTheAssetHasNone() {
		// GIVEN
		Asset mockAsset = Instancio.of(Asset.class)
			.set(Select.field(Asset::getCategory), null)
			.set(Select.field(Asset::getLiabilityType), null)
			.set(Select.field(Asset::getPaymentFrequency), null)
			.create();
		User mockUser = stubExportDataSources(List.of(), List.of(mockAsset), List.of(), List.of());

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUser.getId(), "test@example.com");

		// THEN
		assertThat(expectedExport.assets())
			.singleElement()
			.satisfies(exportedAsset -> {
				assertThat(exportedAsset.category()).isNull();
				assertThat(exportedAsset.liabilityType()).isNull();
				assertThat(exportedAsset.paymentFrequency()).isNull();
			});
	}

	@Test
	void exportUserData_mapsCategoriesTargetsAndCashFlows() {
		// GIVEN
		AssetCategory mockCategory = Instancio.create(AssetCategory.class);
		Target mockTarget = Instancio.create(Target.class);
		CashFlow mockCashFlow = Instancio.create(CashFlow.class);
		User mockUser = stubExportDataSources(
			List.of(mockCategory),
			List.of(),
			List.of(mockTarget),
			List.of(mockCashFlow)
		);

		// WHEN
		var expectedExport = testSubject.exportUserData(mockUser.getId(), "test@example.com");

		// THEN
		assertThat(expectedExport.profile().currency()).isEqualTo(mockUser.getCurrency().name());
		assertThat(expectedExport.profile().locale()).isEqualTo(mockUser.getLocale().name());
		assertThat(expectedExport.categories())
			.singleElement()
			.satisfies(exportedCategory -> {
				assertThat(exportedCategory.id()).isEqualTo(mockCategory.getId());
				assertThat(exportedCategory.name()).isEqualTo(mockCategory.getName());
				assertThat(exportedCategory.type()).isEqualTo(mockCategory.getType().name());
			});
		assertThat(expectedExport.targets())
			.singleElement()
			.satisfies(exportedTarget -> {
				assertThat(exportedTarget.id()).isEqualTo(mockTarget.getId());
				assertThat(exportedTarget.type()).isEqualTo(mockTarget.getType().name());
				assertThat(exportedTarget.deadline()).isEqualTo(mockTarget.getDeadline());
			});
		assertThat(expectedExport.cashFlows())
			.singleElement()
			.satisfies(exportedCashFlow -> {
				assertThat(exportedCashFlow.year()).isEqualTo(mockCashFlow.getYear());
				assertThat(exportedCashFlow.month()).isEqualTo(mockCashFlow.getMonth());
				assertThat(exportedCashFlow.earned()).isEqualByComparingTo(mockCashFlow.getEarned());
				assertThat(exportedCashFlow.spent()).isEqualByComparingTo(mockCashFlow.getSpent());
			});
	}

	@Test
	void exportUserData_throwsNotFoundException_whenUserIsMissing() {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.exportUserData(mockUserId, "test@example.com")
		).isInstanceOf(NotFoundException.class);
	}

	private User stubExportDataSources(
		List<AssetCategory> categories,
		List<Asset> assets,
		List<Target> targets,
		List<CashFlow> cashFlows
	) {
		User mockUser = Instancio.create(User.class);
		UUID mockUserId = mockUser.getId();
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));
		when(categoryRepository.findByUserId(mockUserId)).thenReturn(categories);
		when(assetRepository.findByUserIdOrderByCreatedAtDesc(mockUserId)).thenReturn(assets);
		when(targetRepository.findByUserId(mockUserId)).thenReturn(targets);
		when(cashFlowRepository.findByUserIdOrderByYearDescMonthAsc(mockUserId)).thenReturn(cashFlows);
		return mockUser;
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("missingNameDtos")
	void updateAuth0Name_answersThePinned400_whenNameIsMissing(UpdateNameDTO mockDto) {
		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateAuth0Name("auth0|user", mockDto)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(NAME_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(MISSING_NAME_MESSAGE);
		});
	}

	private static Stream<UpdateNameDTO> missingNameDtos() {
		return Stream.of(new UpdateNameDTO(null), new UpdateNameDTO(BLANK_NAME));
	}

	@Test
	void updateAuth0Name_answersThePinned400_whenNameIsTooLong() {
		// GIVEN
		UpdateNameDTO mockDto = new UpdateNameDTO("a".repeat(MAX_NAME_LENGTH + 1));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateAuth0Name("auth0|user", mockDto)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(NAME_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(NAME_TOO_LONG_MESSAGE);
		});
	}

	@Test
	void updateAuth0Name_sendsTheNameToAuth0_whenValid() throws Auth0Exception {
		// GIVEN
		String mockJwtId = "auth0|user";
		UpdateNameDTO mockDto = new UpdateNameDTO(VALID_NAME);
		ManagementAPI mockMgmt = org.mockito.Mockito.mock(ManagementAPI.class);
		UsersEntity mockUsers = org.mockito.Mockito.mock(UsersEntity.class);
		Request<com.auth0.json.mgmt.users.User> mockRequest = org.mockito.Mockito.mock(Request.class);
		AtomicReference<com.auth0.json.mgmt.users.User> stubbedSentUser = new AtomicReference<>();
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(
			mockUsers.update(
				org.mockito.ArgumentMatchers.eq(mockJwtId),
				org.mockito.ArgumentMatchers.notNull()
			)
		).thenAnswer(invocation -> {
			stubbedSentUser.set(invocation.getArgument(1));
			return mockRequest;
		});

		// WHEN
		testSubject.updateAuth0Name(mockJwtId, mockDto);

		// THEN
		assertThat(stubbedSentUser.get().getName()).isEqualTo(VALID_NAME);
	}

	@Test
	void deleteUser_keepsTheAuth0SubjectOutOfTheVisibleLog() throws Auth0Exception {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		String mockJwtId = "auth0|delete";
		stubAuth0Deletion(mockJwtId);

		// WHEN
		testSubject.deleteUser(mockUserId, mockJwtId);

		// THEN
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL))
			.noneMatch(message -> message.contains(mockJwtId))
			.anyMatch(message -> message.contains(mockUserId.toString()));
		assertThat(loggedMessagesAt(DETAIL_LOG_LEVEL)).anyMatch(message -> message.contains(mockJwtId));
	}

	@Test
	void deleteUser_keepsTheAuth0SubjectOutOfTheErrorLog_whenAuth0Fails() throws Auth0Exception {
		// GIVEN
		String mockJwtId = "auth0|faildelete";
		when(stubAuth0Deletion(mockJwtId).execute()).thenThrow(new Auth0Exception("deletion failed"));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.deleteUser(UUID.randomUUID(), mockJwtId)).isInstanceOf(
			RuntimeException.class
		);
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL)).noneMatch(message ->
			message.contains(mockJwtId)
		);
	}

	@Test
	void updateAuth0Name_keepsTheAuth0SubjectOutOfTheVisibleLog() throws Auth0Exception {
		// GIVEN
		String mockJwtId = "auth0|user";
		stubAuth0NameUpdate(mockJwtId);

		// WHEN
		testSubject.updateAuth0Name(mockJwtId, new UpdateNameDTO(VALID_NAME));

		// THEN
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL)).noneMatch(message ->
			message.contains(mockJwtId)
		);
	}

	@Test
	void updateAuth0Name_keepsTheAuth0SubjectOutOfTheErrorLog_whenAuth0Fails() throws Auth0Exception {
		// GIVEN
		String mockJwtId = "auth0|failedname";
		when(stubAuth0NameUpdate(mockJwtId).execute()).thenThrow(new Auth0Exception("update failed"));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updateAuth0Name(mockJwtId, new UpdateNameDTO(VALID_NAME))
		).isInstanceOf(RuntimeException.class);
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL)).noneMatch(message ->
			message.contains(mockJwtId)
		);
	}

	@Test
	void findOrCreate_keepsTheAuth0SubjectOutOfTheInfoLog() {
		// GIVEN
		String mockJwtId = "auth0|newuser";
		when(userRepository.findByJwtId(mockJwtId)).thenReturn(Optional.empty());
		when(userRepository.saveAndFlush(notNull())).thenReturn(Instancio.create(User.class));

		// WHEN
		testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL)).noneMatch(message ->
			message.contains(mockJwtId)
		);
	}

	@Test
	void findOrCreate_keepsTheAuth0SubjectOutOfTheConcurrencyWarning() {
		// GIVEN
		String mockJwtId = "auth0|concurrent";
		User stubbedUser = Instancio.create(User.class);
		when(userRepository.findByJwtId(mockJwtId))
			.thenReturn(Optional.empty())
			.thenReturn(Optional.of(stubbedUser));
		when(userRepository.saveAndFlush(notNull())).thenThrow(
			new DataIntegrityViolationException("duplicate key")
		);

		// WHEN
		testSubject.findOrCreate(mockJwtId);

		// THEN
		assertThat(loggedMessagesAtOrAbove(VISIBLE_LOG_LEVEL)).noneMatch(message ->
			message.contains(mockJwtId)
		);
		assertThat(loggedMessagesAt(DETAIL_LOG_LEVEL)).anyMatch(message -> message.contains(mockJwtId));
	}

	private Request<Void> stubAuth0Deletion(String jwtId) throws Auth0Exception {
		ManagementAPI mockMgmt = mock(ManagementAPI.class);
		UsersEntity mockUsers = mock(UsersEntity.class);
		Request<Void> mockRequest = mock(Request.class);
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(mockUsers.delete(jwtId)).thenReturn(mockRequest);
		return mockRequest;
	}

	private Request<com.auth0.json.mgmt.users.User> stubAuth0NameUpdate(String jwtId)
		throws Auth0Exception {
		ManagementAPI mockMgmt = mock(ManagementAPI.class);
		UsersEntity mockUsers = mock(UsersEntity.class);
		Request<com.auth0.json.mgmt.users.User> mockRequest = mock(Request.class);
		when(auth0ManagementService.getManagementAPI()).thenReturn(mockMgmt);
		when(mockMgmt.users()).thenReturn(mockUsers);
		when(mockUsers.update(eq(jwtId), notNull())).thenReturn(mockRequest);
		return mockRequest;
	}

	private List<String> loggedMessagesAtOrAbove(Level level) {
		return loggedMessagesWhere(event -> event.getLevel().isGreaterOrEqual(level));
	}

	private List<String> loggedMessagesAt(Level level) {
		return loggedMessagesWhere(event -> event.getLevel() == level);
	}

	private List<String> loggedMessagesWhere(Predicate<ILoggingEvent> condition) {
		return capturedLogLines.list
			.stream()
			.filter(condition)
			.map(ILoggingEvent::getFormattedMessage)
			.toList();
	}

	@Test
	void exceedsPixelLimit_isFalseWithinTheLimit() {
		assertThat(UserService.exceedsPixelLimit(2_000, 2_000)).isFalse();
	}

	@Test
	void exceedsPixelLimit_isTrueAboveTheLimit() {
		assertThat(UserService.exceedsPixelLimit(10_000, 5_000)).isTrue();
	}

	@Test
	void exceedsPixelLimit_isTrueForNonPositiveDimensions() {
		assertThat(UserService.exceedsPixelLimit(0, 100)).isTrue();
		assertThat(UserService.exceedsPixelLimit(100, -1)).isTrue();
	}

	@Test
	void updatePicture_answersThePinned400_whenTheMagicBytesAreNotAnImage() {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(unknownFormatBytes());
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(mockUserId, mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(NOT_AN_IMAGE_MESSAGE);
		});
	}

	@Test
	void updatePicture_answersThePinned400_whenNoReaderClaimsTheImage() {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(truncatedPng());
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(mockUserId, mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(NOT_AN_IMAGE_MESSAGE);
		});
	}

	@Test
	void updatePicture_answersThePinned400_whenTheImageExceedsThePixelLimit() {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(oversizedPng());
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(mockUserId, mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(PICTURE_TOO_LARGE_MESSAGE);
		});
	}

	@Test
	void updatePicture_storesASquareProfilePicture_whenTheImageIsValid() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = Instancio.of(User.class).set(Select.field(User::getPicture), null).create();
		MultipartFile mockFile = buildPictureFile(jpegBytes());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertStoredProfilePictureIsSquare(mockUser);
	}

	@Test
	void updatePicture_answersThePinned400_whenTheFileIsShorterThanAnySignature() {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(PNG_SIGNATURE);
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(mockUserId, mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(NOT_AN_IMAGE_MESSAGE);
		});
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("supportedPictureFormats")
	void updatePicture_storesASquareProfilePicture_whenTheFormatIsSupported(String formatName)
		throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(encodedPicture(formatName));
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertStoredProfilePictureIsSquare(mockUser);
	}

	@Test
	void updatePicture_answersThePinned400_whenTheWebpHeaderHasNoReader() {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(webpHeader());
		UUID mockUserId = UUID.randomUUID();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(mockUserId, mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(NOT_AN_IMAGE_MESSAGE);
		});
	}

	@Test
	void updatePicture_storesThePicture_whenTheExifOrientationIsUnknown() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(jpegWithExifOrientation(ORIENTATION_UNKNOWN));
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertStoredProfilePictureIsSquare(mockUser);
	}

	@Test
	void updatePicture_storesThePicture_whenTheExifIfd0CarriesNoOrientationTag() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(jpegWithForeignExifTag());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertStoredProfilePictureIsSquare(mockUser);
	}

	@Test
	void updatePicture_storesThePicture_whenTheExifOrientationTagHasNoValue() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(jpegWithValuelessExifOrientation());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertStoredProfilePictureIsSquare(mockUser);
	}

	@Test
	void updatePicture_throwsNotFoundException_whenTheUserIsMissing() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		MultipartFile mockFile = buildPictureFile(jpegBytes());
		when(userRepository.findById(mockUserId)).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.updatePicture(mockUserId, mockFile)).isInstanceOf(
			NotFoundException.class
		);
	}

	@ParameterizedTest(name = "{0} with {1}")
	@MethodSource("exifOrientedPictures")
	void updatePicture_orientsThePictureTheExifDemands(Format format, ExifOrientation orientation)
		throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(
			ProfilePictureFixture.picture(format, orientation.value())
		);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		QuadrantLayout expectedLayout = format.carriesExif()
			? orientation.layout()
			: QuadrantLayout.SOURCE;
		BufferedImage expectedPicture = readStoredProfilePicture(mockUser);
		for (Corner corner : Corner.values()) {
			assertThat(ProfilePictureFixture.quadrantOf(expectedPicture, corner))
				.as("%s with %s, %s quadrant", format, orientation, corner)
				.isEqualTo(expectedLayout.at(corner));
		}
	}

	private static Stream<Arguments> exifOrientedPictures() {
		return Arrays.stream(Format.values()).flatMap(format ->
			Arrays.stream(ExifOrientation.values()).map(orientation -> Arguments.of(format, orientation))
		);
	}

	@Test
	void updatePicture_answersThePinned400_whenThePictureCannotBeProcessed() throws IOException {
		// GIVEN
		MultipartFile mockFile = buildPictureFile(ProfilePictureFixture.jpegWithoutScanData());

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.updatePicture(UUID.randomUUID(), mockFile)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(FILE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(UNPROCESSABLE_PICTURE_MESSAGE);
		});
	}

	@Test
	void updatePicture_stripsTheLineBreakOutOfTheLoggedFileName() throws IOException {
		// GIVEN
		UUID mockUserId = UUID.randomUUID();
		User mockUser = userWithoutPicture();
		MultipartFile mockFile = buildPictureFile(
			FORGED_FILE_NAME,
			ProfilePictureFixture.picture(Format.JPEG, ExifOrientation.IDENTITY.value())
		);
		when(userRepository.findById(mockUserId)).thenReturn(Optional.of(mockUser));

		// WHEN
		testSubject.updatePicture(mockUserId, mockFile);

		// THEN
		assertThat(loggedMessagesAt(DETAIL_LOG_LEVEL))
			.anyMatch(message -> message.contains(SANITIZED_FILE_NAME))
			.allMatch(message -> !message.contains(FORGED_LINE_BREAK));
	}

	private static Stream<Arguments> supportedPictureFormats() {
		return Stream.of(Arguments.of(GIF_FORMAT), Arguments.of(BMP_FORMAT));
	}

	private static User userWithoutPicture() {
		return Instancio.of(User.class).set(Select.field(User::getPicture), null).create();
	}

	private static void assertStoredProfilePictureIsSquare(User mockUser) throws IOException {
		BufferedImage expectedImage = readStoredProfilePicture(mockUser);
		assertThat(expectedImage.getWidth()).isEqualTo(PROFILE_PICTURE_SIZE);
		assertThat(expectedImage.getHeight()).isEqualTo(PROFILE_PICTURE_SIZE);
	}

	/**
	 * The stored value is bare base64, the SPA adds the data-URL prefix, and whatever the upload
	 * format was the profile picture is a JPEG.
	 */
	private static BufferedImage readStoredProfilePicture(User mockUser) throws IOException {
		byte[] expectedPicture = Base64.getDecoder().decode(mockUser.getPicture());
		assertThat(Arrays.copyOf(expectedPicture, JPEG_MAGIC_BYTES.length))
			.as("the stored picture starts with the JPEG signature")
			.isEqualTo(JPEG_MAGIC_BYTES);
		return ImageIO.read(new ByteArrayInputStream(expectedPicture));
	}

	private static byte[] encodedPicture(String formatName) throws IOException {
		BufferedImage image = new BufferedImage(
			TINY_PICTURE_WIDTH,
			TINY_PICTURE_HEIGHT,
			BufferedImage.TYPE_INT_RGB
		);
		ByteArrayOutputStream encoded = new ByteArrayOutputStream();
		ImageIO.write(image, formatName, encoded);
		return encoded.toByteArray();
	}

	private static byte[] webpHeader() {
		return Arrays.copyOf(WEBP_RIFF_HEADER, WEBP_HEADER_LENGTH);
	}

	private static byte[] jpegWithExifOrientation(int orientation) throws IOException {
		return jpegWithExifSegment(
			exifSegment(
				EXIF_ORIENTATION_TAG,
				TIFF_TYPE_SHORT,
				ONE_EXIF_ENTRY,
				new byte[] { (byte) orientation }
			)
		);
	}

	private static byte[] jpegWithForeignExifTag() throws IOException {
		return jpegWithExifSegment(
			exifSegment(EXIF_FOREIGN_TAG, TIFF_TYPE_ASCII, ONE_EXIF_ENTRY, new byte[] { 'A', 'u' })
		);
	}

	private static byte[] jpegWithValuelessExifOrientation() throws IOException {
		return jpegWithExifSegment(
			exifSegment(EXIF_ORIENTATION_TAG, TIFF_TYPE_SHORT, NO_EXIF_ENTRY, new byte[0])
		);
	}

	private static byte[] jpegWithExifSegment(byte[] exifSegment) throws IOException {
		byte[] plainJpeg = jpegBytes();
		byte[] framed = new byte[plainJpeg.length + exifSegment.length];
		framed[0] = plainJpeg[0];
		framed[1] = plainJpeg[1];
		System.arraycopy(exifSegment, 0, framed, 2, exifSegment.length);
		System.arraycopy(plainJpeg, 2, framed, 2 + exifSegment.length, plainJpeg.length - 2);
		return framed;
	}

	private static byte[] exifSegment(int tag, int type, int count, byte[] value) {
		ByteArrayOutputStream payload = new ByteArrayOutputStream();
		payload.writeBytes(EXIF_PREFIX);
		payload.writeBytes(TIFF_LITTLE_ENDIAN_HEADER);
		payload.writeBytes(new byte[] { (byte) ONE_EXIF_ENTRY, 0 });
		payload.write(tag & 0xFF);
		payload.write((tag >> 8) & 0xFF);
		payload.write(type);
		payload.write(0);
		writeLittleEndianInt(payload, count);
		byte[] paddedValue = new byte[EXIF_VALUE_LENGTH];
		System.arraycopy(value, 0, paddedValue, 0, Math.min(value.length, EXIF_VALUE_LENGTH));
		payload.writeBytes(paddedValue);
		payload.writeBytes(NO_NEXT_IFD);
		byte[] body = payload.toByteArray();
		int length = body.length + EXIF_SEGMENT_LENGTH_BYTES;
		ByteArrayOutputStream segment = new ByteArrayOutputStream();
		segment.writeBytes(EXIF_SEGMENT_MARKER);
		segment.write((length >> 8) & 0xFF);
		segment.write(length & 0xFF);
		segment.writeBytes(body);
		return segment.toByteArray();
	}

	private static void writeLittleEndianInt(ByteArrayOutputStream out, int value) {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 24) & 0xFF);
	}

	private static byte[] jpegBytes() throws IOException {
		BufferedImage image = new BufferedImage(
			SOURCE_PICTURE_WIDTH,
			SOURCE_PICTURE_HEIGHT,
			BufferedImage.TYPE_INT_RGB
		);
		ByteArrayOutputStream jpeg = new ByteArrayOutputStream();
		ImageIO.write(image, "jpeg", jpeg);
		return jpeg.toByteArray();
	}

	private MultipartFile buildPictureFile(byte[] bytes) {
		return buildPictureFile(DEFAULT_FILE_NAME, bytes);
	}

	private MultipartFile buildPictureFile(String fileName, byte[] bytes) {
		return new MockMultipartFile(FILE_FIELD, fileName, OCTET_STREAM_CONTENT_TYPE, bytes);
	}

	private static byte[] unknownFormatBytes() {
		return new byte[] { 'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E', 0, 0, 0, 0 };
	}

	private static byte[] truncatedPng() {
		byte[] bytes = new byte[TRUNCATED_PNG_LENGTH];
		System.arraycopy(PNG_SIGNATURE, 0, bytes, 0, PNG_MAGIC_BYTES);
		return bytes;
	}

	private static byte[] oversizedPng() {
		ByteArrayOutputStream png = new ByteArrayOutputStream();
		png.writeBytes(PNG_SIGNATURE);
		byte[] chunkType = { 'I', 'H', 'D', 'R' };
		ByteArrayOutputStream chunkData = new ByteArrayOutputStream();
		writeBigEndianInt(chunkData, OVERSIZED_DIMENSION);
		writeBigEndianInt(chunkData, OVERSIZED_DIMENSION);
		chunkData.write(PNG_BIT_DEPTH);
		chunkData.write(PNG_COLOR_TYPE_RGBA);
		chunkData.write(PNG_COMPRESSION_DEFLATE);
		chunkData.write(PNG_FILTER_ADAPTIVE);
		chunkData.write(PNG_INTERLACE_NONE);
		byte[] data = chunkData.toByteArray();
		writeBigEndianInt(png, data.length);
		png.writeBytes(chunkType);
		png.writeBytes(data);
		CRC32 crc = new CRC32();
		crc.update(chunkType);
		crc.update(data);
		writeBigEndianInt(png, (int) crc.getValue());
		return png.toByteArray();
	}

	private static void writeBigEndianInt(ByteArrayOutputStream out, int value) {
		out.write((value >> 24) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write(value & 0xFF);
	}
}
