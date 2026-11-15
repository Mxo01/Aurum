package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.user.model.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
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
class SnapshotValidationServiceTest {

	private static final UUID REQUESTING_USER_ID = UUID.randomUUID();

	private static final UUID OTHER_USER_ID = UUID.randomUUID();

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 20);

	private static final Instant FIXED_INSTANT = FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant();

	private static final ZoneId FIXED_ZONE = ZoneOffset.UTC;

	private static final LocalDate TOMORROW = FIXED_TODAY.plusDays(1);

	private static final LocalDate BEYOND_TIMEZONE_SKEW = FIXED_TODAY.plusDays(2);

	private static final BigDecimal SNAPSHOT_AMOUNT = new BigDecimal("500");

	private static final BigDecimal ZERO_AMOUNT = new BigDecimal("0");

	private static final BigDecimal NEGATIVE_AMOUNT = new BigDecimal("-1");

	private static final BigDecimal POSITIVE_RATE = new BigDecimal("1.5");

	private static final BigDecimal ZERO_RATE = new BigDecimal("0");

	private static final BigDecimal NEGATIVE_RATE = new BigDecimal("-0.5");

	private static final String FUTURE_REFERENCE_DATE_MESSAGE =
		"Reference date cannot be in the future for an automatic liability";

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private SnapshotValidationService testSubject;

	@Test
	void validate_createDto_throwsWhenDtoIsNull() {
		// GIVEN / WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate((CreateSnapshotDTO) null, REQUESTING_USER_ID)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("rejectedCreatePayloads")
	void validate_createDto_rejectsInvalidPayload(
		String scenario,
		Consumer<CreateSnapshotDTO> mutation
	) {
		// GIVEN
		CreateSnapshotDTO mockDto = buildCreateDto(UUID.randomUUID());
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> rejectedCreatePayloads() {
		return Stream.of(
			Arguments.of("null asset id", (Consumer<CreateSnapshotDTO>) dto -> dto.setAssetId(null)),
			Arguments.of(
				"null reference date",
				(Consumer<CreateSnapshotDTO>) dto -> dto.setReferenceDate(null)
			),
			Arguments.of(
				"null amount",
				(Consumer<CreateSnapshotDTO>) dto -> dto.setAmountOriginalCurrency(null)
			),
			Arguments.of(
				"negative amount",
				(Consumer<CreateSnapshotDTO>) dto -> dto.setAmountOriginalCurrency(NEGATIVE_AMOUNT)
			),
			Arguments.of(
				"zero exchange rate",
				(Consumer<CreateSnapshotDTO>) dto -> dto.setExchangeRateToBase(ZERO_RATE)
			),
			Arguments.of(
				"negative exchange rate",
				(Consumer<CreateSnapshotDTO>) dto -> dto.setExchangeRateToBase(NEGATIVE_RATE)
			)
		);
	}

	@Test
	void validate_createDto_acceptsAZeroAmount() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setAmountOriginalCurrency(ZERO_AMOUNT);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_acceptsAMissingExchangeRate() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setExchangeRateToBase(null);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_throwsWhenAssetNotFound() {
		// GIVEN
		CreateSnapshotDTO mockDto = buildCreateDto(UUID.randomUUID());
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_createDto_throwsWhenAssetBelongsToDifferentUser() {
		// GIVEN
		Asset mockAsset = buildAsset(OTHER_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "create: an automatic liability snapshot dated {0} is accepted")
	@MethodSource("toleratedReferenceDates")
	void validate_createDto_acceptsAReferenceDateWithinTheTimezoneSkew(LocalDate referenceDate) {
		// GIVEN
		stubFixedToday();
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.AUTOMATIC);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setReferenceDate(referenceDate);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_rejectsAReferenceDateBeyondTheTimezoneSkew() {
		// GIVEN
		stubFixedToday();
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.AUTOMATIC);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining(FUTURE_REFERENCE_DATE_MESSAGE);
	}

	@Test
	void validate_createDto_acceptsAFutureReferenceDateForAManualLiability() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.MANUAL);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_createDto_acceptsAFutureReferenceDateForAnAssetWithoutLiability() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_snapshotDto_throwsWhenDtoIsNull() {
		// GIVEN / WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate((SnapshotDTO) null, REQUESTING_USER_ID)
		).isInstanceOf(IllegalArgumentException.class);
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("rejectedSnapshotPayloads")
	void validate_snapshotDto_rejectsInvalidPayload(String scenario, Consumer<SnapshotDTO> mutation) {
		// GIVEN
		SnapshotDTO mockDto = buildSnapshotDto(UUID.randomUUID());
		mutation.accept(mockDto);

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	static Stream<Arguments> rejectedSnapshotPayloads() {
		return Stream.of(
			Arguments.of("null asset id", (Consumer<SnapshotDTO>) dto -> dto.setAssetId(null)),
			Arguments.of(
				"null reference date",
				(Consumer<SnapshotDTO>) dto -> dto.setReferenceDate(null)
			),
			Arguments.of(
				"null amount",
				(Consumer<SnapshotDTO>) dto -> dto.setAmountOriginalCurrency(null)
			),
			Arguments.of(
				"negative amount",
				(Consumer<SnapshotDTO>) dto -> dto.setAmountOriginalCurrency(NEGATIVE_AMOUNT)
			),
			Arguments.of(
				"zero exchange rate",
				(Consumer<SnapshotDTO>) dto -> dto.setExchangeRateToBase(ZERO_RATE)
			),
			Arguments.of(
				"negative exchange rate",
				(Consumer<SnapshotDTO>) dto -> dto.setExchangeRateToBase(NEGATIVE_RATE)
			)
		);
	}

	@Test
	void validate_snapshotDto_acceptsAZeroAmount() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setAmountOriginalCurrency(ZERO_AMOUNT);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_snapshotDto_acceptsAMissingExchangeRate() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setExchangeRateToBase(null);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_snapshotDto_throwsWhenAssetNotFound() {
		// GIVEN
		SnapshotDTO mockDto = buildSnapshotDto(UUID.randomUUID());
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@Test
	void validate_snapshotDto_throwsWhenAssetBelongsToDifferentUser() {
		// GIVEN
		Asset mockAsset = buildAsset(OTHER_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID)).isInstanceOf(
			IllegalArgumentException.class
		);
	}

	@ParameterizedTest(name = "update: an automatic liability snapshot dated {0} is accepted")
	@MethodSource("toleratedReferenceDates")
	void validate_snapshotDto_acceptsAReferenceDateWithinTheTimezoneSkew(LocalDate referenceDate) {
		// GIVEN
		stubFixedToday();
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.AUTOMATIC);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setReferenceDate(referenceDate);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_snapshotDto_rejectsAReferenceDateBeyondTheTimezoneSkew() {
		// GIVEN
		stubFixedToday();
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.AUTOMATIC);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() -> testSubject.validate(mockDto, REQUESTING_USER_ID))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessageContaining(FUTURE_REFERENCE_DATE_MESSAGE);
	}

	@Test
	void validate_snapshotDto_acceptsAFutureReferenceDateForAManualLiability() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, LiabilityType.MANUAL);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setReferenceDate(BEYOND_TIMEZONE_SKEW);
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@Test
	void validate_snapshotDto_doesNotThrowForValidDto() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	static Stream<LocalDate> toleratedReferenceDates() {
		return Stream.of(FIXED_TODAY, TOMORROW);
	}

	private void stubFixedToday() {
		when(clock.instant()).thenReturn(FIXED_INSTANT);
		when(clock.getZone()).thenReturn(FIXED_ZONE);
	}

	private static Asset buildAsset(UUID ownerId, LiabilityType liabilityType) {
		User mockOwner = Instancio.of(User.class).set(Select.field(User::getId), ownerId).create();
		Asset mockAsset = new Asset();
		mockAsset.setId(UUID.randomUUID());
		mockAsset.setUser(mockOwner);
		mockAsset.setLiabilityType(liabilityType);
		return mockAsset;
	}

	private static CreateSnapshotDTO buildCreateDto(UUID assetId) {
		CreateSnapshotDTO dto = new CreateSnapshotDTO();
		dto.setAssetId(assetId);
		dto.setReferenceDate(FIXED_TODAY);
		dto.setAmountOriginalCurrency(SNAPSHOT_AMOUNT);
		dto.setExchangeRateToBase(POSITIVE_RATE);
		return dto;
	}

	private static SnapshotDTO buildSnapshotDto(UUID assetId) {
		SnapshotDTO dto = new SnapshotDTO();
		dto.setAssetId(assetId);
		dto.setReferenceDate(FIXED_TODAY);
		dto.setAmountOriginalCurrency(SNAPSHOT_AMOUNT);
		dto.setExchangeRateToBase(POSITIVE_RATE);
		return dto;
	}
}
