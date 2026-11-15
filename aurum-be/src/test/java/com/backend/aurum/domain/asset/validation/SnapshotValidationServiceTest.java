package com.backend.aurum.domain.asset.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.dto.CreateSnapshotDTO;
import com.backend.aurum.domain.asset.dto.SnapshotDTO;
import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.shared.exception.AccessDeniedException;
import com.backend.aurum.domain.shared.exception.ValidationException;
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

	private static final BigDecimal AMOUNT_AT_FOUR_DECIMALS = new BigDecimal("500.0001");

	private static final BigDecimal AMOUNT_OVER_FOUR_DECIMALS = new BigDecimal("500.00001");

	private static final BigDecimal AMOUNT_AT_NINETEEN_DIGITS = new BigDecimal(
		"123456789012345.9999"
	);

	private static final BigDecimal AMOUNT_OVER_FIFTEEN_INTEGER_DIGITS = new BigDecimal(
		"1234567890123456"
	);

	private static final BigDecimal RATE_AT_SIX_DECIMALS = new BigDecimal("0.123456");

	private static final BigDecimal RATE_OVER_SIX_DECIMALS = new BigDecimal("0.1234567");

	private static final BigDecimal RATE_AT_CEILING = new BigDecimal("1000000");

	private static final BigDecimal RATE_OVER_CEILING = new BigDecimal("1000001");

	private static final String ASSET_ID_FIELD = "assetId";

	private static final String REFERENCE_DATE_FIELD = "referenceDate";

	private static final String AMOUNT_FIELD = "amountOriginalCurrency";

	private static final String EXCHANGE_RATE_FIELD = "exchangeRateToBase";

	private static final String NO_DATA_MESSAGE = "Snapshot data is required";

	private static final String MISSING_ASSET_MESSAGE = "Asset is required";

	private static final String MISSING_REFERENCE_DATE_MESSAGE = "Value date is required";

	private static final String MISSING_AMOUNT_MESSAGE = "Amount is required";

	private static final String NEGATIVE_AMOUNT_MESSAGE = "Amount cannot be negative";

	private static final String NON_POSITIVE_EXCHANGE_RATE_MESSAGE =
		"Exchange rate must be greater than zero";

	private static final String AMOUNT_SCALE_MESSAGE =
		"Amount cannot have more than 4 decimal places";

	private static final String AMOUNT_TOO_LARGE_MESSAGE = "Amount is too large";

	private static final String RATE_SCALE_MESSAGE =
		"Exchange rate cannot have more than 6 decimal places";

	private static final String RATE_TOO_LARGE_MESSAGE = "Exchange rate is too large";

	private static final String ASSET_NOT_FOUND_MESSAGE = "Asset not found";

	private static final String FUTURE_REFERENCE_DATE_MESSAGE =
		"Value date cannot be in the future for an automatic liability";

	private static final String FOREIGN_ASSET_MESSAGE = "This asset does not belong to you";

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private Clock clock;

	@InjectMocks
	private SnapshotValidationService testSubject;

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("createRejections")
	void validate_createDto_answersThePinnedFieldAndMessage(CreateRejection createRejection) {
		// GIVEN
		CreateSnapshotDTO mockDto = createRejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(createRejection.expectedField());
			assertThat(expectedException.getMessage()).isEqualTo(createRejection.expectedMessage());
		});
	}

	static Stream<CreateRejection> createRejections() {
		return Stream.of(
			new CreateRejection(
				"400 without a field when the snapshot data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new CreateRejection(
				"400 on the asset when it is missing",
				createDtoWith(dto -> dto.setAssetId(null)),
				ASSET_ID_FIELD,
				MISSING_ASSET_MESSAGE
			),
			new CreateRejection(
				"400 on the value date when it is missing",
				createDtoWith(dto -> dto.setReferenceDate(null)),
				REFERENCE_DATE_FIELD,
				MISSING_REFERENCE_DATE_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it is missing",
				createDtoWith(dto -> dto.setAmountOriginalCurrency(null)),
				AMOUNT_FIELD,
				MISSING_AMOUNT_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it is negative",
				createDtoWith(dto -> dto.setAmountOriginalCurrency(NEGATIVE_AMOUNT)),
				AMOUNT_FIELD,
				NEGATIVE_AMOUNT_MESSAGE
			),
			new CreateRejection(
				"400 on the exchange rate when it is zero",
				createDtoWith(dto -> dto.setExchangeRateToBase(ZERO_RATE)),
				EXCHANGE_RATE_FIELD,
				NON_POSITIVE_EXCHANGE_RATE_MESSAGE
			),
			new CreateRejection(
				"400 on the exchange rate when it is negative",
				createDtoWith(dto -> dto.setExchangeRateToBase(NEGATIVE_RATE)),
				EXCHANGE_RATE_FIELD,
				NON_POSITIVE_EXCHANGE_RATE_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it has more than four decimals",
				createDtoWith(dto -> dto.setAmountOriginalCurrency(AMOUNT_OVER_FOUR_DECIMALS)),
				AMOUNT_FIELD,
				AMOUNT_SCALE_MESSAGE
			),
			new CreateRejection(
				"400 on the amount when it does not fit the money column",
				createDtoWith(dto -> dto.setAmountOriginalCurrency(AMOUNT_OVER_FIFTEEN_INTEGER_DIGITS)),
				AMOUNT_FIELD,
				AMOUNT_TOO_LARGE_MESSAGE
			),
			new CreateRejection(
				"400 on the exchange rate when it has more than six decimals",
				createDtoWith(dto -> dto.setExchangeRateToBase(RATE_OVER_SIX_DECIMALS)),
				EXCHANGE_RATE_FIELD,
				RATE_SCALE_MESSAGE
			),
			new CreateRejection(
				"400 on the exchange rate when it is past the ceiling",
				createDtoWith(dto -> dto.setExchangeRateToBase(RATE_OVER_CEILING)),
				EXCHANGE_RATE_FIELD,
				RATE_TOO_LARGE_MESSAGE
			)
		);
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("snapshotRejections")
	void validate_snapshotDto_answersThePinnedFieldAndMessage(SnapshotRejection snapshotRejection) {
		// GIVEN
		SnapshotDTO mockDto = snapshotRejection.dto();

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(snapshotRejection.expectedField());
			assertThat(expectedException.getMessage()).isEqualTo(snapshotRejection.expectedMessage());
		});
	}

	static Stream<SnapshotRejection> snapshotRejections() {
		return Stream.of(
			new SnapshotRejection(
				"400 without a field when the snapshot data is missing",
				null,
				null,
				NO_DATA_MESSAGE
			),
			new SnapshotRejection(
				"400 on the asset when it is missing",
				snapshotDtoWith(dto -> dto.setAssetId(null)),
				ASSET_ID_FIELD,
				MISSING_ASSET_MESSAGE
			),
			new SnapshotRejection(
				"400 on the value date when it is missing",
				snapshotDtoWith(dto -> dto.setReferenceDate(null)),
				REFERENCE_DATE_FIELD,
				MISSING_REFERENCE_DATE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the amount when it is missing",
				snapshotDtoWith(dto -> dto.setAmountOriginalCurrency(null)),
				AMOUNT_FIELD,
				MISSING_AMOUNT_MESSAGE
			),
			new SnapshotRejection(
				"400 on the amount when it is negative",
				snapshotDtoWith(dto -> dto.setAmountOriginalCurrency(NEGATIVE_AMOUNT)),
				AMOUNT_FIELD,
				NEGATIVE_AMOUNT_MESSAGE
			),
			new SnapshotRejection(
				"400 on the exchange rate when it is zero",
				snapshotDtoWith(dto -> dto.setExchangeRateToBase(ZERO_RATE)),
				EXCHANGE_RATE_FIELD,
				NON_POSITIVE_EXCHANGE_RATE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the exchange rate when it is negative",
				snapshotDtoWith(dto -> dto.setExchangeRateToBase(NEGATIVE_RATE)),
				EXCHANGE_RATE_FIELD,
				NON_POSITIVE_EXCHANGE_RATE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the amount when it has more than four decimals",
				snapshotDtoWith(dto -> dto.setAmountOriginalCurrency(AMOUNT_OVER_FOUR_DECIMALS)),
				AMOUNT_FIELD,
				AMOUNT_SCALE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the amount when it does not fit the money column",
				snapshotDtoWith(dto -> dto.setAmountOriginalCurrency(AMOUNT_OVER_FIFTEEN_INTEGER_DIGITS)),
				AMOUNT_FIELD,
				AMOUNT_TOO_LARGE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the exchange rate when it has more than six decimals",
				snapshotDtoWith(dto -> dto.setExchangeRateToBase(RATE_OVER_SIX_DECIMALS)),
				EXCHANGE_RATE_FIELD,
				RATE_SCALE_MESSAGE
			),
			new SnapshotRejection(
				"400 on the exchange rate when it is past the ceiling",
				snapshotDtoWith(dto -> dto.setExchangeRateToBase(RATE_OVER_CEILING)),
				EXCHANGE_RATE_FIELD,
				RATE_TOO_LARGE_MESSAGE
			)
		);
	}

	@Test
	void validate_createDto_answersThePinnedFieldAndMessage_whenTheAssetDoesNotExist() {
		// GIVEN
		CreateSnapshotDTO mockDto = buildCreateDto(UUID.randomUUID());
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(ASSET_ID_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(ASSET_NOT_FOUND_MESSAGE);
		});
	}

	@Test
	void validate_snapshotDto_answersThePinnedFieldAndMessage_whenTheAssetDoesNotExist() {
		// GIVEN
		SnapshotDTO mockDto = buildSnapshotDto(UUID.randomUUID());
		when(assetRepository.findById(mockDto.getAssetId())).thenReturn(Optional.empty());

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(ASSET_ID_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(ASSET_NOT_FOUND_MESSAGE);
		});
	}

	@Test
	void validate_createDto_answersAFieldless403_whenTheAssetBelongsToAnotherUser() {
		// GIVEN
		Asset mockAsset = buildAsset(OTHER_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(AccessDeniedException.class, expectedException ->
			assertThat(expectedException.getMessage()).isEqualTo(FOREIGN_ASSET_MESSAGE)
		);
	}

	@Test
	void validate_snapshotDto_answersAFieldless403_whenTheAssetBelongsToAnotherUser() {
		// GIVEN
		Asset mockAsset = buildAsset(OTHER_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(AccessDeniedException.class, expectedException ->
			assertThat(expectedException.getMessage()).isEqualTo(FOREIGN_ASSET_MESSAGE)
		);
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
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(REFERENCE_DATE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(FUTURE_REFERENCE_DATE_MESSAGE);
		});
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
		assertThatThrownBy(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).isInstanceOfSatisfying(ValidationException.class, expectedException -> {
			assertThat(expectedException.getField()).isEqualTo(REFERENCE_DATE_FIELD);
			assertThat(expectedException.getMessage()).isEqualTo(FUTURE_REFERENCE_DATE_MESSAGE);
		});
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
	void validate_snapshotDto_acceptsAValidDto() {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "create: {0}")
	@MethodSource("acceptedAmounts")
	void validate_createDto_acceptsAmountsAndRatesWithinTheirColumns(
		AcceptedAmounts acceptedAmounts
	) {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		CreateSnapshotDTO mockDto = buildCreateDto(mockAsset.getId());
		mockDto.setAmountOriginalCurrency(acceptedAmounts.amount());
		mockDto.setExchangeRateToBase(acceptedAmounts.rate());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	@ParameterizedTest(name = "update: {0}")
	@MethodSource("acceptedAmounts")
	void validate_snapshotDto_acceptsAmountsAndRatesWithinTheirColumns(
		AcceptedAmounts acceptedAmounts
	) {
		// GIVEN
		Asset mockAsset = buildAsset(REQUESTING_USER_ID, null);
		SnapshotDTO mockDto = buildSnapshotDto(mockAsset.getId());
		mockDto.setAmountOriginalCurrency(acceptedAmounts.amount());
		mockDto.setExchangeRateToBase(acceptedAmounts.rate());
		when(assetRepository.findById(mockAsset.getId())).thenReturn(Optional.of(mockAsset));

		// WHEN / THEN
		assertThatCode(() ->
			testSubject.validate(mockDto, REQUESTING_USER_ID)
		).doesNotThrowAnyException();
	}

	static Stream<AcceptedAmounts> acceptedAmounts() {
		return Stream.of(
			new AcceptedAmounts(
				"an amount with four decimals and a rate with six",
				AMOUNT_AT_FOUR_DECIMALS,
				RATE_AT_SIX_DECIMALS
			),
			new AcceptedAmounts(
				"the widest amount the money column holds",
				AMOUNT_AT_NINETEEN_DIGITS,
				POSITIVE_RATE
			),
			new AcceptedAmounts("a rate exactly at the ceiling", SNAPSHOT_AMOUNT, RATE_AT_CEILING)
		);
	}

	static Stream<Arguments> toleratedReferenceDates() {
		return Stream.of(Arguments.of(FIXED_TODAY), Arguments.of(TOMORROW));
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

	private static CreateSnapshotDTO createDtoWith(Consumer<CreateSnapshotDTO> mutation) {
		CreateSnapshotDTO dto = buildCreateDto(UUID.randomUUID());
		mutation.accept(dto);
		return dto;
	}

	private static SnapshotDTO snapshotDtoWith(Consumer<SnapshotDTO> mutation) {
		SnapshotDTO dto = buildSnapshotDto(UUID.randomUUID());
		mutation.accept(dto);
		return dto;
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

	private record CreateRejection(
		String scenario,
		CreateSnapshotDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record SnapshotRejection(
		String scenario,
		SnapshotDTO dto,
		String expectedField,
		String expectedMessage
	) {}

	private record AcceptedAmounts(String scenario, BigDecimal amount, BigDecimal rate) {}
}
