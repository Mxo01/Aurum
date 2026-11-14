package com.backend.aurum.domain.asset.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.backend.aurum.domain.asset.model.SchedulerRun;
import com.backend.aurum.domain.asset.model.SchedulerRunStatus;
import com.backend.aurum.domain.asset.repository.SchedulerRunRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

@ExtendWith(MockitoExtension.class)
class SchedulerRunRecorderTest {

	private static final String JOB_NAME = "processAutomaticLiabilityPayments";

	private static final Instant FIXED_INSTANT = Instant.parse("2026-11-14T00:00:00Z");

	private static final LocalDateTime FIXED_NOW = LocalDateTime.ofInstant(
		FIXED_INSTANT,
		ZoneOffset.UTC
	);

	private static final String STUBBED_FAILURE_MESSAGE = "trace table unavailable";

	private static final BiConsumer<SchedulerRunRecorder, UUID> closingComplete =
		SchedulerRunRecorder::complete;

	private static final BiConsumer<SchedulerRunRecorder, UUID> closingFail =
		SchedulerRunRecorder::fail;

	@Mock
	private SchedulerRunRepository schedulerRunRepository;

	@Mock
	private PlatformTransactionManager transactionManager;

	private SchedulerRunRecorder testSubject;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
		testSubject = new SchedulerRunRecorder(schedulerRunRepository, transactionManager, clock);
	}

	private static Stream<Arguments> closingOperations() {
		return Stream.of(
			Arguments.of(SchedulerRunStatus.COMPLETED, closingComplete),
			Arguments.of(SchedulerRunStatus.FAILED, closingFail)
		);
	}

	private static Stream<BiConsumer<SchedulerRunRecorder, UUID>> closingCalls() {
		return Stream.of(closingComplete, closingFail);
	}

	private SchedulerRun stubbedStartedRun() {
		SchedulerRun stubbedRun = new SchedulerRun();
		stubbedRun.setId(UUID.randomUUID());
		return stubbedRun;
	}

	@Test
	void start_savesStartedRunAndReturnsItsId() {
		// GIVEN
		SchedulerRun mockRun = stubbedStartedRun();
		when(schedulerRunRepository.save(any(SchedulerRun.class))).thenAnswer(invocation -> {
			SchedulerRun runToSave = invocation.getArgument(0, SchedulerRun.class);
			runToSave.setId(mockRun.getId());
			return runToSave;
		});

		// WHEN
		UUID expectedRunId = testSubject.start(JOB_NAME);

		// THEN
		assertThat(expectedRunId).isEqualTo(mockRun.getId());
		ArgumentCaptor<SchedulerRun> runCaptor = ArgumentCaptor.forClass(SchedulerRun.class);
		verify(schedulerRunRepository).save(runCaptor.capture());
		SchedulerRun expectedRun = runCaptor.getValue();
		assertThat(expectedRun.getJobName()).isEqualTo(JOB_NAME);
		assertThat(expectedRun.getStatus()).isEqualTo(SchedulerRunStatus.STARTED);
		assertThat(expectedRun.getStartedAt()).isEqualTo(FIXED_NOW);
		assertThat(expectedRun.getFinishedAt()).isNull();
	}

	@ParameterizedTest
	@EnumSource(StartFailure.class)
	void start_returnsNull_whenRecordingFails(StartFailure startFailure) {
		// GIVEN
		RuntimeException mockFailure = new RuntimeException(STUBBED_FAILURE_MESSAGE);
		switch (startFailure) {
			case TRANSACTION -> when(
				transactionManager.getTransaction(any(TransactionDefinition.class))
			).thenThrow(mockFailure);
			case SAVE -> when(schedulerRunRepository.save(any(SchedulerRun.class))).thenThrow(
				mockFailure
			);
		}

		// WHEN
		UUID expectedRunId = testSubject.start(JOB_NAME);

		// THEN
		assertThat(expectedRunId).isNull();
	}

	@ParameterizedTest
	@MethodSource("closingOperations")
	void closing_marksRunWithStatusAndFinishedAt(
		SchedulerRunStatus mockStatus,
		BiConsumer<SchedulerRunRecorder, UUID> closing
	) {
		// GIVEN
		SchedulerRun mockRun = stubbedStartedRun();
		when(schedulerRunRepository.findById(mockRun.getId())).thenReturn(Optional.of(mockRun));

		// WHEN
		closing.accept(testSubject, mockRun.getId());

		// THEN
		ArgumentCaptor<SchedulerRun> runCaptor = ArgumentCaptor.forClass(SchedulerRun.class);
		verify(schedulerRunRepository).save(runCaptor.capture());
		SchedulerRun expectedRun = runCaptor.getValue();
		assertThat(expectedRun).isSameAs(mockRun);
		assertThat(expectedRun.getStatus()).isEqualTo(mockStatus);
		assertThat(expectedRun.getFinishedAt()).isEqualTo(FIXED_NOW);
	}

	@ParameterizedTest
	@MethodSource("closingCalls")
	void closing_doesNothing_whenRunIdIsNull(BiConsumer<SchedulerRunRecorder, UUID> closing) {
		// WHEN
		closing.accept(testSubject, null);

		// THEN
		verifyNoInteractions(schedulerRunRepository);
	}

	@ParameterizedTest
	@MethodSource("closingCalls")
	void closing_leavesRunUntouched_whenTraceNoLongerExists(
		BiConsumer<SchedulerRunRecorder, UUID> closing
	) {
		// GIVEN
		UUID mockRunId = UUID.randomUUID();
		when(schedulerRunRepository.findById(mockRunId)).thenReturn(Optional.empty());

		// WHEN
		closing.accept(testSubject, mockRunId);

		// THEN
		verify(schedulerRunRepository).findById(mockRunId);
		verify(schedulerRunRepository, never()).save(any(SchedulerRun.class));
	}

	@ParameterizedTest
	@MethodSource("closingCalls")
	void closing_swallowsFailure_whenTraceCannotBeRead(
		BiConsumer<SchedulerRunRecorder, UUID> closing
	) {
		// GIVEN
		UUID mockRunId = UUID.randomUUID();
		when(schedulerRunRepository.findById(mockRunId)).thenThrow(
			new RuntimeException(STUBBED_FAILURE_MESSAGE)
		);

		// WHEN
		closing.accept(testSubject, mockRunId);

		// THEN
		verify(schedulerRunRepository, never()).save(any(SchedulerRun.class));
	}

	@ParameterizedTest
	@MethodSource("closingOperations")
	void closing_swallowsFailure_whenTraceCannotBeWritten(
		SchedulerRunStatus mockStatus,
		BiConsumer<SchedulerRunRecorder, UUID> closing
	) {
		// GIVEN
		SchedulerRun mockRun = stubbedStartedRun();
		when(schedulerRunRepository.findById(mockRun.getId())).thenReturn(Optional.of(mockRun));
		when(schedulerRunRepository.save(mockRun)).thenThrow(
			new RuntimeException(STUBBED_FAILURE_MESSAGE)
		);

		// WHEN
		closing.accept(testSubject, mockRun.getId());

		// THEN
		SchedulerRunStatus expectedStatus = mockRun.getStatus();
		assertThat(expectedStatus).isEqualTo(mockStatus);
	}

	private enum StartFailure {
		TRANSACTION,
		SAVE
	}
}
