package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityPaymentStatus;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.exception.NotFoundException;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LiabilityPaymentServiceTest {

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 20);

	private static final LocalDateTime FIXED_NOW = LocalDateTime.of(FIXED_TODAY, LocalTime.MIDNIGHT);

	private static final Clock FIXED_CLOCK = Clock.fixed(
		FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
		ZoneOffset.UTC
	);

	private static final Currency BASE_CURRENCY = Currency.EUR;

	private static final Currency FOREIGN_CURRENCY = Currency.USD;

	private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("200");

	private static final BigDecimal OUTSTANDING_BALANCE = new BigDecimal("1000");

	private static final LocalDate THREE_MONTHS_AGO = LocalDate.of(2026, 1, 20);

	private static final LocalDate MONTH_AGO = LocalDate.of(2026, 3, 20);

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	private SnapshotTable snapshotTable;

	private LiabilityPaymentService testSubject;

	@BeforeEach
	void setUp() {
		snapshotTable = new SnapshotTable();
		snapshotTable.install(snapshotRepository);
		SnapshotService snapshotService = new SnapshotService(snapshotRepository, assetRepository);
		CurrencyConversionService conversionService = new CurrencyConversionService(
			exchangeRateProvider
		);
		testSubject = new LiabilityPaymentService(
			assetRepository,
			snapshotRepository,
			snapshotService,
			conversionService,
			FIXED_CLOCK
		);
	}

	private static Stream<Arguments> nonPositivePaymentAmounts() {
		return Stream.of(
			Arguments.of("absent", null),
			Arguments.of("zero", BigDecimal.ZERO),
			Arguments.of("negative", new BigDecimal("-50"))
		);
	}

	private static Stream<Arguments> nonPositiveValuations() {
		return Stream.of(
			Arguments.of("absent", null),
			Arguments.of("zero", BigDecimal.ZERO),
			Arguments.of("negative", new BigDecimal("-50"))
		);
	}

	private Asset buildLiability(PaymentFrequency frequency, BigDecimal paymentAmount) {
		User mockUser = new User();
		mockUser.setId(UUID.randomUUID());
		mockUser.setCurrency(BASE_CURRENCY);

		Asset liability = new Asset();
		liability.setId(UUID.randomUUID());
		liability.setName("Car loan");
		liability.setUser(mockUser);
		liability.setOriginalCurrency(BASE_CURRENCY);
		liability.setLiabilityType(LiabilityType.AUTOMATIC);
		liability.setPaymentFrequency(frequency);
		liability.setPaymentAmount(paymentAmount);
		return liability;
	}

	private Snapshot buildSnapshot(Asset asset, BigDecimal amount, LocalDate referenceDate) {
		Snapshot snapshot = new Snapshot();
		snapshot.setAsset(asset);
		snapshot.setAmountOriginalCurrency(amount);
		snapshot.setExchangeRateToBase(BigDecimal.ONE);
		snapshot.setReferenceDate(referenceDate);
		return snapshot;
	}

	private User buildUser(Currency currency) {
		User user = new User();
		user.setId(UUID.randomUUID());
		user.setCurrency(currency);
		return user;
	}

	private void givenLiability(Asset asset) {
		when(assetRepository.findById(asset.getId())).thenReturn(Optional.of(asset));
	}

	private void givenPersistedAsset() {
		lenient().when(assetRepository.save(any(Asset.class))).thenAnswer(returnsFirstArg());
	}

	private LiabilityPaymentResult payQuietly(Asset asset) {
		return testSubject.pay(asset.getId(), FIXED_TODAY);
	}

	private static ExpectedPayment payment(String dueDate, String amount) {
		return new ExpectedPayment(LocalDate.parse(dueDate), new BigDecimal(amount));
	}

	private void assertScheduledPayments(List<ExpectedPayment> expectedPayments) {
		List<Snapshot> scheduledRows = snapshotTable.scheduledRows();
		assertThat(scheduledRows)
			.extracting(Snapshot::getReferenceDate)
			.containsExactlyElementsOf(expectedPayments.stream().map(ExpectedPayment::dueDate).toList());
		assertThat(scheduledRows)
			.extracting(row -> row.getAmountOriginalCurrency().stripTrailingZeros())
			.containsExactlyElementsOf(
				expectedPayments
					.stream()
					.map(expectedPayment -> expectedPayment.amount().stripTrailingZeros())
					.toList()
			);
	}

	private record ExpectedPayment(LocalDate dueDate, BigDecimal amount) {}

	private static final class SnapshotTable {

		private final List<Snapshot> rows = new ArrayList<>();

		private void install(SnapshotRepository repository) {
			lenient()
				.when(repository.save(any(Snapshot.class)))
				.thenAnswer(invocation -> {
					Snapshot row = invocation.getArgument(0, Snapshot.class);
					if (row.getId() == null) {
						row.setId(UUID.randomUUID());
					}
					rows.add(row);
					return row;
				});
			lenient()
				.when(repository.findFirstByAssetIdOrderByReferenceDateDescIdAsc(any(UUID.class)))
				.thenAnswer(invocation ->
					Optional.ofNullable(latestFor(invocation.getArgument(0, UUID.class)))
				);
			lenient()
				.when(
					repository.findFirstByAssetIdAndSourceAndReferenceDateBetween(
						any(UUID.class),
						any(SnapshotSource.class),
						any(LocalDate.class),
						any(LocalDate.class)
					)
				)
				.thenAnswer(invocation ->
					firstUserRowIn(
						invocation.getArgument(0, UUID.class),
						invocation.getArgument(1, SnapshotSource.class),
						invocation.getArgument(2, LocalDate.class),
						invocation.getArgument(3, LocalDate.class)
					)
				);
		}

		private void seed(Snapshot row) {
			if (row.getId() == null) {
				row.setId(UUID.randomUUID());
			}
			rows.add(row);
		}

		private List<Snapshot> rows() {
			return List.copyOf(rows);
		}

		private List<Snapshot> scheduledRows() {
			return rows
				.stream()
				.filter(row -> row.getSource() == SnapshotSource.SCHEDULER)
				.toList();
		}

		private Snapshot latestFor(UUID assetId) {
			return rows
				.stream()
				.filter(row -> assetId.equals(row.getAsset().getId()))
				.max(
					Comparator.comparing(Snapshot::getReferenceDate).thenComparing(
						Snapshot::getId,
						Comparator.reverseOrder()
					)
				)
				.orElse(null);
		}

		private Optional<Snapshot> firstUserRowIn(
			UUID assetId,
			SnapshotSource source,
			LocalDate start,
			LocalDate end
		) {
			return rows
				.stream()
				.filter(row -> assetId.equals(row.getAsset().getId()))
				.filter(row -> row.getSource() == source)
				.filter(row -> !row.getReferenceDate().isBefore(start))
				.filter(row -> !row.getReferenceDate().isAfter(end))
				.findFirst();
		}
	}

	@Test
	void pay_throwsNotFound_whenTheAssetDoesNotExist() {
		// GIVEN
		UUID mockAssetId = UUID.randomUUID();
		when(assetRepository.findById(mockAssetId)).thenReturn(Optional.empty());

		// WHEN
		Throwable expectedFailure = catchThrowable(() -> testSubject.pay(mockAssetId, FIXED_TODAY));

		// THEN
		assertThat(expectedFailure).isInstanceOf(NotFoundException.class);
	}

	@Test
	void pay_skipsTheLiability_whenItIsAlreadySettled() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setSettledAt(FIXED_NOW);
		givenLiability(mockAsset);

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.reason()).isEqualTo(LiabilityPaymentService.SKIP_ALREADY_SETTLED);
		verify(assetRepository, never()).save(any(Asset.class));
	}

	@Test
	void pay_skipsTheLiability_whenItHasNoPaymentFrequency() {
		// GIVEN
		Asset mockAsset = buildLiability(null, MONTHLY_PAYMENT);
		givenLiability(mockAsset);

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.reason()).isEqualTo(
			LiabilityPaymentService.SKIP_NO_PAYMENT_FREQUENCY
		);
		verify(assetRepository, never()).save(any(Asset.class));
	}

	@ParameterizedTest(name = "payment amount {0}")
	@MethodSource("nonPositivePaymentAmounts")
	void pay_skipsTheLiability_whenThePaymentAmountIsNotPositive(
		String stubbedCase,
		BigDecimal stubbedPaymentAmount
	) {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, stubbedPaymentAmount);
		givenLiability(mockAsset);

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.reason()).isEqualTo(
			LiabilityPaymentService.SKIP_NO_POSITIVE_PAYMENT_AMOUNT
		);
		verify(assetRepository, never()).save(any(Asset.class));
	}

	@Test
	void pay_skipsTheLiability_whenItHasNoValuationSnapshot() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.reason()).isEqualTo(
			LiabilityPaymentService.SKIP_NO_VALUATION_SNAPSHOT
		);
		verify(assetRepository, never()).save(any(Asset.class));
	}

	@ParameterizedTest(name = "valuation {0}")
	@MethodSource("nonPositiveValuations")
	void pay_settlesTheLiability_whenTheLatestValuationIsNotPositive(
		String stubbedCase,
		BigDecimal stubbedValuation
	) {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, stubbedValuation, MONTH_AGO));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.SKIPPED);
		assertThat(expectedResult.reason()).isEqualTo(
			LiabilityPaymentService.SKIP_NO_OUTSTANDING_BALANCE
		);
		assertThat(mockAsset.getSettledAt()).isEqualTo(FIXED_NOW);
		assertThat(mockAsset.getNextDueDate()).isNull();
		assertThat(mockAsset.getPaymentAnchorDay()).isNull();
		assertThat(snapshotTable.scheduledRows()).isEmpty();
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_derivesTheScheduleAndPays_whenTheDueDateIsToday() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, MONTH_AGO));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(expectedResult.paymentsApplied()).isEqualTo(1);
		assertThat(expectedResult.outstandingBalance()).isEqualByComparingTo("800");
		assertThat(expectedResult.nextDueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
		assertThat(mockAsset.getPaymentAnchorDay()).isEqualTo((short) 20);
		assertThat(mockAsset.getNextDueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
		assertScheduledPayments(List.of(payment("2026-04-20", "800")));
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_persistsTheDerivedSchedule_whenTheNextPeriodIsInTheFuture() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);
		givenPersistedAsset();
		LocalDate valuationDate = LocalDate.of(2026, 4, 15);
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, valuationDate));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.NOT_DUE);
		assertThat(expectedResult.nextDueDate()).isEqualTo(LocalDate.of(2026, 5, 15));
		assertThat(mockAsset.getPaymentAnchorDay()).isEqualTo((short) 15);
		assertThat(snapshotTable.scheduledRows()).isEmpty();
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_doesNotPersistTheAsset_whenThePersistedScheduleIsNotDue() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setNextDueDate(LocalDate.of(2026, 5, 10));
		mockAsset.setPaymentAnchorDay((short) 10);
		givenLiability(mockAsset);
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, LocalDate.of(2026, 4, 10)));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.NOT_DUE);
		assertThat(expectedResult.nextDueDate()).isEqualTo(LocalDate.of(2026, 5, 10));
		assertThat(snapshotTable.scheduledRows()).isEmpty();
		verify(assetRepository, never()).save(any(Asset.class));
	}

	@Test
	void pay_persistsTheAnchor_whenTheDueDateIsPersistedWithoutOne() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setNextDueDate(LocalDate.of(2026, 5, 10));
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, LocalDate.of(2026, 4, 10)));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.NOT_DUE);
		assertThat(expectedResult.nextDueDate()).isEqualTo(LocalDate.of(2026, 5, 10));
		assertThat(mockAsset.getPaymentAnchorDay()).isEqualTo((short) 10);
		assertThat(snapshotTable.scheduledRows()).isEmpty();
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_paysEveryMissedPeriodOnItsOwnDueDate() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, THREE_MONTHS_AGO));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(expectedResult.paymentsApplied()).isEqualTo(3);
		assertThat(expectedResult.outstandingBalance()).isEqualByComparingTo("400");
		assertThat(expectedResult.nextDueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
		assertScheduledPayments(
			List.of(
				payment("2026-02-20", "800"),
				payment("2026-03-20", "600"),
				payment("2026-04-20", "400")
			)
		);
		assertThat(mockAsset.getNextDueDate()).isEqualTo(LocalDate.of(2026, 5, 20));
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_clampsTheLastPaymentToTheOutstandingBalance() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, new BigDecimal("300"));
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, new BigDecimal("500"), LocalDate.of(2026, 1, 10)));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(expectedResult.paymentsApplied()).isEqualTo(2);
		assertThat(expectedResult.outstandingBalance()).isEqualByComparingTo("0");
		assertThat(expectedResult.nextDueDate()).isNull();
		assertScheduledPayments(List.of(payment("2026-02-10", "200"), payment("2026-03-10", "0")));
		assertThat(mockAsset.getSettledAt()).isEqualTo(FIXED_NOW);
		assertThat(mockAsset.getNextDueDate()).isNull();
		assertThat(mockAsset.getPaymentAnchorDay()).isNull();
		verify(assetRepository).save(mockAsset);
	}

	@Test
	void pay_keepsTheAnchorDayAcrossShorterMonths() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, new BigDecimal("250"));
		mockAsset.setNextDueDate(LocalDate.of(2026, 1, 31));
		mockAsset.setPaymentAnchorDay((short) 31);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, LocalDate.of(2026, 1, 31)));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertScheduledPayments(
			List.of(
				payment("2026-01-31", "750"),
				payment("2026-02-28", "500"),
				payment("2026-03-31", "250")
			)
		);
		assertThat(mockAsset.getNextDueDate()).isEqualTo(LocalDate.of(2026, 4, 30));
	}

	@Test
	void pay_resolvesEachRateOnItsDueDateInOneProviderCall() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setUser(buildUser(FOREIGN_CURRENCY));
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, THREE_MONTHS_AGO));

		Map<LocalDate, BigDecimal> stubbedRates = new LinkedHashMap<>();
		stubbedRates.put(LocalDate.of(2026, 2, 20), new BigDecimal("1.10"));
		stubbedRates.put(LocalDate.of(2026, 3, 20), new BigDecimal("1.20"));
		stubbedRates.put(LocalDate.of(2026, 4, 20), new BigDecimal("1.30"));
		List<Collection<LocalDate>> stubbedRequests = new ArrayList<>();
		when(
			exchangeRateProvider.getRates(
				eq(BASE_CURRENCY.getValue()),
				eq(FOREIGN_CURRENCY.getValue()),
				anyCollection()
			)
		).thenAnswer(invocation -> {
			stubbedRequests.add(invocation.getArgument(2));
			return stubbedRates;
		});

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		assertThat(stubbedRequests).hasSize(1);
		assertThat(stubbedRequests.get(0)).containsExactlyElementsOf(stubbedRates.keySet());
		assertThat(snapshotTable.scheduledRows())
			.extracting(row -> row.getExchangeRateToBase().stripTrailingZeros())
			.containsExactlyElementsOf(
				stubbedRates.values().stream().map(BigDecimal::stripTrailingZeros).toList()
			);
	}

	@Test
	void pay_usesTheOneRateWithoutAProviderCall_whenTheAssetIsInTheUserCurrency() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, THREE_MONTHS_AGO));

		// WHEN
		payQuietly(mockAsset);

		// THEN
		verifyNoInteractions(exchangeRateProvider);
		assertThat(snapshotTable.scheduledRows())
			.extracting(row -> row.getExchangeRateToBase().stripTrailingZeros())
			.containsOnly(BigDecimal.ONE.stripTrailingZeros());
	}

	@Test
	void pay_usesTheAssetCurrency_whenTheLiabilityHasNoUser() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setUser(null);
		givenLiability(mockAsset);
		givenPersistedAsset();
		snapshotTable.seed(buildSnapshot(mockAsset, OUTSTANDING_BALANCE, MONTH_AGO));

		// WHEN
		LiabilityPaymentResult expectedResult = payQuietly(mockAsset);

		// THEN
		assertThat(expectedResult.status()).isEqualTo(LiabilityPaymentStatus.PAID);
		verifyNoInteractions(exchangeRateProvider);
		assertScheduledPayments(List.of(payment("2026-04-20", "800")));
	}
}
