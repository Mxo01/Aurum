package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.Asset;
import com.backend.aurum.domain.asset.model.LiabilityType;
import com.backend.aurum.domain.asset.model.PaymentFrequency;
import com.backend.aurum.domain.asset.model.Snapshot;
import com.backend.aurum.domain.asset.model.SnapshotSource;
import com.backend.aurum.domain.asset.repository.AssetRepository;
import com.backend.aurum.domain.asset.repository.SnapshotRepository;
import com.backend.aurum.domain.shared.currency.CurrencyConversionService;
import com.backend.aurum.domain.shared.port.ExchangeRateProvider;
import com.backend.aurum.domain.user.enums.Currency;
import com.backend.aurum.domain.user.model.User;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith({ MockitoExtension.class, OutputCaptureExtension.class })
class LiabilitySchedulerServiceTest {

	private static final LocalDate FIXED_TODAY = LocalDate.of(2026, 4, 20);

	private static final Clock FIXED_CLOCK = Clock.fixed(
		FIXED_TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(),
		ZoneOffset.UTC
	);

	private static final Currency BASE_CURRENCY = Currency.EUR;

	private static final BigDecimal MONTHLY_PAYMENT = new BigDecimal("200");

	private static final BigDecimal OUTSTANDING_BALANCE = new BigDecimal("1000");

	private static final String OVERRIDDEN_CRON = "0 30 2 * * *";

	private static final String STUBBED_FAILURE_MESSAGE = "ledger unavailable";

	private static final String SKIPPED_LOG_PREFIX = "Skipped automatic liability ";

	private static final String SUMMARY_LOG_PREFIX =
		"Automatic liability payment processor completed: ";

	private static final String CLAMPED_LOG_MARKER = "configured per period over";

	private static final String WARN_LEVEL = "WARN";

	private static final BigDecimal LOW_BALANCE = new BigDecimal("100");

	private static final BigDecimal PAYMENT_ABOVE_BALANCE = new BigDecimal("720");

	@Mock
	private AssetRepository assetRepository;

	@Mock
	private SnapshotRepository snapshotRepository;

	@Mock
	private ExchangeRateProvider exchangeRateProvider;

	@Mock
	private SchedulerRunRecorder schedulerRunRecorder;

	private UUID stubbedRunId;

	private SnapshotTable snapshotTable;

	private SnapshotService snapshotService;

	private LiabilitySchedulerService testSubject;

	@BeforeEach
	void setUp() {
		stubbedRunId = UUID.randomUUID();
		snapshotTable = new SnapshotTable();
		snapshotTable.install(snapshotRepository);
		snapshotService = new SnapshotService(snapshotRepository, assetRepository);
		CurrencyConversionService conversionService = new CurrencyConversionService(
			exchangeRateProvider
		);
		LiabilityPaymentService paymentService = new LiabilityPaymentService(
			assetRepository,
			snapshotRepository,
			snapshotService,
			conversionService,
			FIXED_CLOCK
		);
		testSubject = new LiabilitySchedulerService(
			assetRepository,
			paymentService,
			schedulerRunRecorder,
			FIXED_CLOCK
		);
		lenient()
			.when(schedulerRunRecorder.start(LiabilitySchedulerService.JOB_NAME))
			.thenReturn(stubbedRunId);
		lenient().when(assetRepository.save(any(Asset.class))).thenAnswer(returnsFirstArg());
	}

	@AfterEach
	void tearDown() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	private static Stream<Arguments> missedPeriodScenarios() {
		return Stream.of(
			Arguments.of(
				"weekly, four missed weeks",
				new PaymentScenario(
					PaymentFrequency.WEEKLY,
					LocalDate.of(2026, 3, 23),
					new BigDecimal("100"),
					List.of(
						payment("2026-03-30", "900"),
						payment("2026-04-06", "800"),
						payment("2026-04-13", "700"),
						payment("2026-04-20", "600")
					),
					LocalDate.of(2026, 4, 27)
				)
			),
			Arguments.of(
				"monthly, three missed months",
				new PaymentScenario(
					PaymentFrequency.MONTHLY,
					LocalDate.of(2026, 1, 20),
					MONTHLY_PAYMENT,
					List.of(
						payment("2026-02-20", "800"),
						payment("2026-03-20", "600"),
						payment("2026-04-20", "400")
					),
					LocalDate.of(2026, 5, 20)
				)
			),
			Arguments.of(
				"monthly, month-end anchor stable across February",
				new PaymentScenario(
					PaymentFrequency.MONTHLY,
					LocalDate.of(2025, 12, 31),
					new BigDecimal("300"),
					List.of(
						payment("2026-01-31", "700"),
						payment("2026-02-28", "400"),
						payment("2026-03-31", "100")
					),
					LocalDate.of(2026, 4, 30)
				)
			),
			Arguments.of(
				"yearly, two missed years",
				new PaymentScenario(
					PaymentFrequency.YEARLY,
					LocalDate.of(2023, 6, 15),
					new BigDecimal("250"),
					List.of(payment("2024-06-15", "750"), payment("2025-06-15", "500")),
					LocalDate.of(2026, 6, 15)
				)
			),
			Arguments.of(
				"monthly, next period still in the future",
				new PaymentScenario(
					PaymentFrequency.MONTHLY,
					LocalDate.of(2026, 4, 15),
					MONTHLY_PAYMENT,
					List.of(),
					LocalDate.of(2026, 5, 15)
				)
			)
		);
	}

	private static Stream<Arguments> effectiveCrons() {
		return Stream.of(
			Arguments.of("default", LiabilitySchedulerService.JOB_CRON),
			Arguments.of("overridden", OVERRIDDEN_CRON)
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

	private Asset givenValuatedLiability(
		PaymentFrequency frequency,
		BigDecimal paymentAmount,
		LocalDate valuationDate
	) {
		return givenValuatedLiability(frequency, paymentAmount, OUTSTANDING_BALANCE, valuationDate);
	}

	private Asset givenValuatedLiability(
		PaymentFrequency frequency,
		BigDecimal paymentAmount,
		BigDecimal outstanding,
		LocalDate valuationDate
	) {
		Asset liability = buildLiability(frequency, paymentAmount);
		snapshotTable.seed(buildSnapshot(liability, outstanding, valuationDate));
		return liability;
	}

	private void givenAutomaticLiabilities(Asset... liabilities) {
		when(
			assetRepository.findIdsByLiabilityTypeAndSettledAtIsNull(LiabilityType.AUTOMATIC)
		).thenReturn(Stream.of(liabilities).map(Asset::getId).toList());
		for (Asset liability : liabilities) {
			when(assetRepository.findById(liability.getId())).thenReturn(Optional.of(liability));
		}
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

	private static List<String> linesContaining(CapturedOutput output, String marker) {
		return output
			.getAll()
			.lines()
			.filter(line -> line.contains(marker))
			.toList();
	}

	private record PaymentScenario(
		PaymentFrequency frequency,
		LocalDate valuationDate,
		BigDecimal paymentAmount,
		List<ExpectedPayment> expectedRows,
		LocalDate expectedNextDue
	) {}

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
					firstRowIn(
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

		private Optional<Snapshot> firstRowIn(
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

	@ParameterizedTest(name = "{0}")
	@MethodSource("missedPeriodScenarios")
	void processAutomaticLiabilityPayments_paysEachMissedPeriodOnItsOwnDueDate(
		String stubbedCase,
		PaymentScenario scenario,
		CapturedOutput output
	) {
		// GIVEN
		Asset mockAsset = givenValuatedLiability(
			scenario.frequency(),
			scenario.paymentAmount(),
			scenario.valuationDate()
		);
		givenAutomaticLiabilities(mockAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertScheduledPayments(scenario.expectedRows());
		assertThat(snapshotTable.scheduledRows()).allSatisfy(row ->
			assertThat(row.getAsset()).isSameAs(mockAsset)
		);
		assertThat(mockAsset.getNextDueDate()).isEqualTo(scenario.expectedNextDue());
		assertThat(mockAsset.getSettledAt()).isNull();
		verify(schedulerRunRecorder).complete(stubbedRunId);
	}

	@Test
	void processAutomaticLiabilityPayments_leavesTheUserSnapshotUntouched_whenPayingTheSameDate() {
		// GIVEN
		Asset mockAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		mockAsset.setNextDueDate(LocalDate.of(2026, 4, 10));
		mockAsset.setPaymentAnchorDay((short) 10);
		givenAutomaticLiabilities(mockAsset);
		LocalDate referenceDate = LocalDate.of(2026, 4, 10);
		Snapshot mockUserSnapshot = snapshotService.saveOrUpdate(
			mockAsset,
			OUTSTANDING_BALANCE,
			referenceDate,
			BigDecimal.ONE
		);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertThat(snapshotTable.rows()).hasSize(2).contains(mockUserSnapshot);
		Snapshot expectedUserSnapshot = snapshotTable.rows().get(0);
		assertThat(expectedUserSnapshot).isSameAs(mockUserSnapshot);
		assertThat(expectedUserSnapshot.getSource()).isEqualTo(SnapshotSource.USER);
		assertThat(expectedUserSnapshot.getAmountOriginalCurrency()).isEqualByComparingTo(
			OUTSTANDING_BALANCE
		);
		assertThat(expectedUserSnapshot.getReferenceDate()).isEqualTo(referenceDate);
		assertThat(expectedUserSnapshot.getId()).isNotNull();
		assertScheduledPayments(List.of(payment("2026-04-10", "800")));
		assertThat(snapshotTable.scheduledRows().get(0).getReferenceDate()).isEqualTo(referenceDate);
	}

	@Test
	void processAutomaticLiabilityPayments_countsPaidSkippedAndNotDueSeparately(
		CapturedOutput output
	) {
		// GIVEN
		Asset mockPaidAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		Asset mockSkippedAsset = givenValuatedLiability(
			null,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		Asset mockNotDueAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 4, 15)
		);
		givenAutomaticLiabilities(mockPaidAsset, mockSkippedAsset, mockNotDueAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertScheduledPayments(List.of(payment("2026-04-20", "800")));
		assertThat(snapshotTable.scheduledRows()).allSatisfy(row ->
			assertThat(row.getAsset()).isSameAs(mockPaidAsset)
		);
		assertThat(output).contains(
			SKIPPED_LOG_PREFIX + mockSkippedAsset.getId() + ": no payment frequency"
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "1 paid, 1 skipped, 1 not due, 0 failed out of 3 total"
		);
		verify(schedulerRunRecorder).complete(stubbedRunId);
	}

	@Test
	void processAutomaticLiabilityPayments_logsOneLinePerSkippedLiabilityWithItsReason(
		CapturedOutput output
	) {
		// GIVEN
		Asset mockUnconfiguredAsset = buildLiability(null, MONTHLY_PAYMENT);
		Asset mockUnvaluatedAsset = buildLiability(PaymentFrequency.MONTHLY, MONTHLY_PAYMENT);
		givenAutomaticLiabilities(mockUnconfiguredAsset, mockUnvaluatedAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertThat(output).containsOnlyOnce(
			SKIPPED_LOG_PREFIX + mockUnconfiguredAsset.getId() + ": no payment frequency"
		);
		assertThat(output).containsOnlyOnce(
			SKIPPED_LOG_PREFIX + mockUnvaluatedAsset.getId() + ": no valuation snapshot"
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "0 paid, 2 skipped, 0 not due, 0 failed out of 2 total"
		);
		assertThat(snapshotTable.scheduledRows()).isEmpty();
	}

	@Test
	void processAutomaticLiabilityPayments_logsSettledLiability_whenTheLastPeriodClearsTheBalance(
		CapturedOutput output
	) {
		// GIVEN
		Asset mockAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			new BigDecimal("300"),
			new BigDecimal("600"),
			LocalDate.of(2026, 2, 20)
		);
		givenAutomaticLiabilities(mockAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertScheduledPayments(List.of(payment("2026-03-20", "300"), payment("2026-04-20", "0")));
		assertThat(mockAsset.getSettledAt()).isNotNull();
		assertThat(mockAsset.getNextDueDate()).isNull();
		assertThat(output).contains(
			"Automatic liability " + mockAsset.getId() + " paid 2 period(s) and is settled"
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "1 paid, 0 skipped, 0 not due, 0 failed out of 1 total"
		);
	}

	@Test
	void processAutomaticLiabilityPayments_continuesAfterAFailingLiabilityAndRecordsFailedRun(
		CapturedOutput output
	) {
		// GIVEN
		Asset mockFailingAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		Asset mockPaidAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		when(
			assetRepository.findIdsByLiabilityTypeAndSettledAtIsNull(LiabilityType.AUTOMATIC)
		).thenReturn(List.of(mockFailingAsset.getId(), mockPaidAsset.getId()));
		when(assetRepository.findById(mockFailingAsset.getId())).thenThrow(
			new IllegalStateException(STUBBED_FAILURE_MESSAGE)
		);
		when(assetRepository.findById(mockPaidAsset.getId())).thenReturn(Optional.of(mockPaidAsset));

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertScheduledPayments(List.of(payment("2026-04-20", "800")));
		assertThat(snapshotTable.scheduledRows()).allSatisfy(row ->
			assertThat(row.getAsset()).isSameAs(mockPaidAsset)
		);
		assertThat(output).contains(
			"Failed to process payment for liability asset " + mockFailingAsset.getId()
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "1 paid, 0 skipped, 0 not due, 1 failed out of 2 total"
		);
		verify(schedulerRunRecorder).fail(stubbedRunId);
		verify(schedulerRunRecorder, never()).complete(any());
	}

	@Test
	void processAutomaticLiabilityPayments_logsTheClampedPaymentAsAWarning(CapturedOutput output) {
		// GIVEN
		Asset mockAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			PAYMENT_ABOVE_BALANCE,
			LOW_BALANCE,
			LocalDate.of(2026, 3, 20)
		);
		givenAutomaticLiabilities(mockAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		List<String> clampedLines = linesContaining(output, CLAMPED_LOG_MARKER);
		assertThat(clampedLines).hasSize(1);
		assertThat(clampedLines.get(0))
			.contains(WARN_LEVEL)
			.contains("Automatic liability " + mockAsset.getId() + " applied 100 of the 720");
		assertThat(output).contains(
			"Automatic liability " + mockAsset.getId() + " paid 1 period(s) and is settled"
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "1 paid, 0 skipped, 0 not due, 0 failed out of 1 total"
		);
		assertThat(mockAsset.getSettledAt()).isNotNull();
	}

	@Test
	void processAutomaticLiabilityPayments_logsNoWarning_whenThePaymentIsNotClamped(
		CapturedOutput output
	) {
		// GIVEN
		Asset mockAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		givenAutomaticLiabilities(mockAsset);

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertThat(linesContaining(output, CLAMPED_LOG_MARKER)).isEmpty();
		assertThat(linesContaining(output, WARN_LEVEL)).isEmpty();
		assertThat(output).contains(
			"Automatic liability " + mockAsset.getId() + " paid 1 period(s); balance 800"
		);
		assertThat(output).contains(
			SUMMARY_LOG_PREFIX + "1 paid, 0 skipped, 0 not due, 0 failed out of 1 total"
		);
	}

	@Test
	void processAutomaticLiabilityPayments_recordsFailedRun_whenTheSelectionFails() {
		// GIVEN
		when(
			assetRepository.findIdsByLiabilityTypeAndSettledAtIsNull(LiabilityType.AUTOMATIC)
		).thenThrow(new IllegalStateException(STUBBED_FAILURE_MESSAGE));

		// WHEN
		Throwable expectedFailure = catchThrowable(testSubject::processAutomaticLiabilityPayments);

		// THEN
		assertThat(expectedFailure).isInstanceOf(IllegalStateException.class);
		assertThat(expectedFailure).hasMessage(STUBBED_FAILURE_MESSAGE);
		verify(schedulerRunRecorder).fail(stubbedRunId);
		verify(schedulerRunRecorder, never()).complete(any());
	}

	@Test
	void processAutomaticLiabilityPayments_registersNoTransactionSynchronization() {
		// GIVEN
		Asset mockAsset = givenValuatedLiability(
			PaymentFrequency.MONTHLY,
			MONTHLY_PAYMENT,
			LocalDate.of(2026, 3, 20)
		);
		givenAutomaticLiabilities(mockAsset);
		TransactionSynchronizationManager.initSynchronization();

		// WHEN
		testSubject.processAutomaticLiabilityPayments();

		// THEN
		assertThat(TransactionSynchronizationManager.getSynchronizations()).isEmpty();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("effectiveCrons")
	void logRegistration_logsTheEffectiveCron(
		String stubbedCase,
		String stubbedCron,
		CapturedOutput output
	) {
		// GIVEN
		ReflectionTestUtils.setField(testSubject, "cron", stubbedCron);

		// WHEN
		testSubject.logRegistration();

		// THEN
		assertThat(output).contains(
			"Scheduled job " +
				LiabilitySchedulerService.JOB_NAME +
				" registered: cron '" +
				stubbedCron +
				"' zone " +
				LiabilitySchedulerService.JOB_ZONE
		);
	}

	@Test
	void scheduledJob_declaresTheDefaultCronAsItsPropertyFallback() throws NoSuchMethodException {
		// GIVEN
		Method job = LiabilitySchedulerService.class.getMethod("processAutomaticLiabilityPayments");

		// WHEN
		Scheduled expectedSchedule = job.getAnnotation(Scheduled.class);

		// THEN
		assertThat(expectedSchedule).isNotNull();
		assertThat(expectedSchedule.cron()).isEqualTo(
			"${aurum.liability.scheduler.cron:" + LiabilitySchedulerService.JOB_CRON + "}"
		);
		assertThat(expectedSchedule.zone()).isEqualTo(LiabilitySchedulerService.JOB_ZONE);
	}
}
