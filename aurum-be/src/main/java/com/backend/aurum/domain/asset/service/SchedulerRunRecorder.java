package com.backend.aurum.domain.asset.service;

import com.backend.aurum.domain.asset.model.SchedulerRun;
import com.backend.aurum.domain.asset.model.SchedulerRunStatus;
import com.backend.aurum.domain.asset.repository.SchedulerRunRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Leaves one durable trace row per scheduled job execution.
 *
 * <p>Every write runs in its own transaction (REQUIRES_NEW) and swallows its own failures: a
 * rolled-back job batch still leaves its FAILED trace, and a broken trace table never breaks the
 * job that is being traced.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerRunRecorder {

	private final SchedulerRunRepository schedulerRunRepository;
	private final PlatformTransactionManager transactionManager;
	private final Clock clock;

	public UUID start(String jobName) {
		try {
			return inOwnTransaction(() -> {
				SchedulerRun run = new SchedulerRun();
				run.setJobName(jobName);
				run.setStartedAt(LocalDateTime.now(clock));
				run.setStatus(SchedulerRunStatus.STARTED);
				return schedulerRunRepository.save(run).getId();
			});
		} catch (RuntimeException e) {
			log.warn(
				"SchedulerRunRecorder#start - Cannot record run trace for job={}: {}",
				jobName,
				e.getMessage(),
				e
			);
			return null;
		}
	}

	public void complete(UUID runId) {
		close(runId, SchedulerRunStatus.COMPLETED);
	}

	public void fail(UUID runId) {
		close(runId, SchedulerRunStatus.FAILED);
	}

	private void close(UUID runId, SchedulerRunStatus status) {
		if (runId == null) {
			log.debug(
				"SchedulerRunRecorder#close - No run trace to close, status={} not recorded",
				status
			);
			return;
		}

		try {
			runInOwnTransaction(() -> {
				schedulerRunRepository
					.findById(runId)
					.ifPresentOrElse(
						run -> {
							run.setStatus(status);
							run.setFinishedAt(LocalDateTime.now(clock));
							schedulerRunRepository.save(run);
						},
						() ->
							log.warn(
								"SchedulerRunRecorder#close - Run trace runId={} no longer exists, status={} not recorded",
								runId,
								status
							)
					);
			});
		} catch (RuntimeException e) {
			log.warn(
				"SchedulerRunRecorder#close - Cannot close run trace runId={} with status={}: {}",
				runId,
				status,
				e.getMessage(),
				e
			);
		}
	}

	private <T> T inOwnTransaction(Supplier<T> work) {
		return newTransactionTemplate().execute(status -> work.get());
	}

	private void runInOwnTransaction(Runnable work) {
		newTransactionTemplate().executeWithoutResult(status -> work.run());
	}

	private TransactionTemplate newTransactionTemplate() {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template;
	}
}
