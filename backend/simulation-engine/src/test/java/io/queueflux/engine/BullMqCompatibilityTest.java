package io.queueflux.engine;

import io.queueflux.engine.api.*;
import io.queueflux.model.scenario.*;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Compatibility test suite documenting which BullMQ semantics QueueFlux simulates and which are
 * explicitly out of scope. Supported behaviors are asserted with concrete scenarios; unsupported
 * behaviors are marked @Disabled with a note explaining the semantic gap.
 *
 * All tests use fixed seeds for reproducibility. See docs/BULLMQ_PROFILE.md for the full
 * boundary description and the rationale for each exclusion.
 */
@DisplayName("BullMQ Compatibility Suite")
class BullMqCompatibilityTest {

    // ── Supported behaviors ────────────────────────────────────────────────────

    @Test
    @DisplayName("SUPPORTED: FIFO queue dispatches jobs strictly in arrival order")
    void fifo_queue_dispatches_in_arrival_order() {
        // BullMQ default: jobs in a queue are processed in FIFO order unless a priority is set.
        var scenario = new ScenarioDefinition(1, "fifo-compat", 5_000_000L, 42L,
                List.of(new QueueDefinition("q", 20, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("pool", "q", 1, 1, ServiceTimeModel.constant(100_000L))),
                List.of(new WorkloadDefinition("load", "q", 0, 1_000_000L, ArrivalModel.constantRate(5))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        var startedJobNumbers = result.trace().stream()
                .filter(e -> "JOB_STARTED".equals(e.type()))
                .map(e -> Long.parseLong(e.jobId().substring(e.jobId().lastIndexOf(':') + 1)))
                .toList();
        for (int i = 1; i < startedJobNumbers.size(); i++) {
            assertThat(startedJobNumbers.get(i)).isGreaterThan(startedJobNumbers.get(i - 1));
        }
    }

    @Test
    @DisplayName("SUPPORTED: Token-bucket rate limiter enforces minimum gap between job starts")
    void token_bucket_rate_limiter_enforces_start_gap() {
        // BullMQ rate limiter (rateLimiter option): limits starts to N per duration window.
        // QueueFlux models this as a global token-bucket: ratePerSecond=2, burstCapacity=1
        // means at most 2 starts per second → consecutive starts must be >= 500_000 µs apart.
        var scenario = new ScenarioDefinition(1, "rate-compat", 5_000_000L, 7L,
                List.of(new QueueDefinition("q", 100, SchedulingDiscipline.FIFO,
                        RetryPolicy.disabled(), new RateLimitPolicy(2, 1))),
                List.of(new WorkerPoolDefinition("pool", "q", 10, 1, ServiceTimeModel.constant(10_000L))),
                List.of(new WorkloadDefinition("load", "q", 0, 5_000_000L, ArrivalModel.constantRate(20))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        var startTimes = result.trace().stream()
                .filter(e -> "JOB_STARTED".equals(e.type()))
                .mapToLong(TraceEvent::timeMicros).toArray();
        assertThat(startTimes).isNotEmpty();
        for (int i = 1; i < startTimes.length; i++) {
            assertThat(startTimes[i] - startTimes[i - 1])
                    .as("gap between start %d and start %d", i - 1, i)
                    .isGreaterThanOrEqualTo(500_000L);
        }
    }

    @Test
    @DisplayName("SUPPORTED: maxAttempts caps total processing attempts per job")
    void max_attempts_caps_processing_attempts() {
        // BullMQ: job.opts.attempts limits the total number of processing attempts (including the first).
        // QueueFlux: RetryPolicy.maxAttempts — with failureProbability=1.0, every attempt fails.
        int maxAttempts = 3;
        var scenario = new ScenarioDefinition(1, "attempts-compat", 3_000_000L, 99L,
                List.of(new QueueDefinition("q", 20, SchedulingDiscipline.FIFO,
                        new RetryPolicy(maxAttempts, 1_000L, 10_000L, 2.0, 0.0), RateLimitPolicy.unlimited())),
                List.of(new WorkerPoolDefinition("pool", "q", 1, 1, ServiceTimeModel.constant(10_000L), 1.0, List.of())),
                List.of(new WorkloadDefinition("load", "q", 0, 100_000L, ArrivalModel.constantRate(5))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        assertThat(result.deadLetteredJobs()).isPositive();
        // With 100% failure rate, each dead-lettered job must have consumed exactly maxAttempts attempts.
        assertThat(result.failedAttempts()).isEqualTo(result.deadLetteredJobs() * maxAttempts);
    }

    @Test
    @DisplayName("SUPPORTED: Exponential backoff doubles retry delay on each successive attempt")
    void exponential_backoff_doubles_delay_per_attempt() {
        // BullMQ default backoff strategy: exponential, doubling the base delay on each attempt.
        // QueueFlux: RetryPolicy(multiplier=2.0, jitterFraction=0.0) reproduces this exactly.
        // Single job (ratePerSecond=1, endMicros=200_000µs → 1 arrival at t=0), maxAttempts=4.
        var scenario = new ScenarioDefinition(1, "backoff-compat", 10_000_000L, 55L,
                List.of(new QueueDefinition("q", 10, SchedulingDiscipline.FIFO,
                        new RetryPolicy(4, 100_000L, 5_000_000L, 2.0, 0.0), RateLimitPolicy.unlimited())),
                List.of(new WorkerPoolDefinition("pool", "q", 1, 1, ServiceTimeModel.constant(10_000L), 1.0, List.of())),
                List.of(new WorkloadDefinition("load", "q", 0, 200_000L, ArrivalModel.constantRate(1))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));

        // Collect retry-scheduled and retry-ready times for the single job "w0:load:0".
        String jobId = "w0:load:0";
        var scheduledTimes = result.trace().stream()
                .filter(e -> "JOB_RETRY_SCHEDULED".equals(e.type()) && jobId.equals(e.jobId()))
                .mapToLong(TraceEvent::timeMicros).toArray();
        var readyTimes = result.trace().stream()
                .filter(e -> "JOB_RETRY_READY".equals(e.type()) && jobId.equals(e.jobId()))
                .mapToLong(TraceEvent::timeMicros).toArray();

        assertThat(scheduledTimes).hasSize(3); // 3 retries before the 4th attempt dead-letters
        assertThat(readyTimes).hasSize(3);

        long delay0 = readyTimes[0] - scheduledTimes[0]; // expected: 100_000 µs
        long delay1 = readyTimes[1] - scheduledTimes[1]; // expected: 200_000 µs
        long delay2 = readyTimes[2] - scheduledTimes[2]; // expected: 400_000 µs

        assertThat(delay1).isCloseTo(delay0 * 2, Offset.offset(2L));
        assertThat(delay2).isCloseTo(delay1 * 2, Offset.offset(2L));
    }

    @Test
    @DisplayName("SUPPORTED: Worker outage reduces new-dispatch capacity without interrupting active jobs")
    void worker_outage_reduces_dispatch_capacity_without_interrupting_active_jobs() {
        // BullMQ note: worker outages are NOT stalled-job recovery — they model planned capacity
        // reductions (e.g. rolling deploys). Jobs already being processed are unaffected.
        var scenario = new ScenarioDefinition(1, "outage-compat", 4_000_000L, 8L,
                List.of(new QueueDefinition("q", 100, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("pool", "q", 2, 1, ServiceTimeModel.constant(200_000L), 0,
                        List.of(new WorkerOutage("planned", 0L, 1_500_000L, 2)))),
                List.of(new WorkloadDefinition("load", "q", 0, 4_000_000L, ArrivalModel.constantRate(5))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        // No jobs should start while all 2 workers are in the outage window [0, 1_500_000).
        long startedDuringOutage = result.trace().stream()
                .filter(e -> "JOB_STARTED".equals(e.type()) && e.timeMicros() < 1_500_000L)
                .count();
        assertThat(startedDuringOutage).isZero();
        // Jobs do accumulate in the queue and are processed after the outage ends.
        assertThat(result.completedJobs()).isPositive();
    }

    // ── Unsupported behaviors ──────────────────────────────────────────────────

    @Disabled("NOT IMPLEMENTED: BullMQ stalled-job detection relies on Redis lock expiry. " +
              "When a worker process dies, BullMQ re-queues the job after the lock TTL. " +
              "QueueFlux's virtual-time model has no equivalent of lock expiry or process death. " +
              "See docs/BULLMQ_PROFILE.md for details.")
    @Test
    void stalled_job_detection_and_lock_renewal_not_supported() {}

    @Disabled("NOT IMPLEMENTED: BullMQ uses Redis Lua scripts and distributed locks to coordinate " +
              "workers across multiple processes. QueueFlux is a single-process, in-memory simulator " +
              "and does not model distributed locking or inter-process coordination.")
    @Test
    void distributed_locks_and_lock_token_recovery_not_supported() {}

    @Disabled("NOT IMPLEMENTED: BullMQ supports job groups, parent/child flows, and dependency graphs. " +
              "QueueFlux's flat workload model has no concept of job relationships or flow dependencies.")
    @Test
    void job_groups_and_parent_child_flows_not_supported() {}

    @Disabled("NOT IMPLEMENTED: BullMQ supports repeatable jobs (cron-style) and job deduplication keys. " +
              "QueueFlux workloads define arrival processes (POISSON / CONSTANT) but have no deduplication " +
              "or scheduling-by-cron semantics.")
    @Test
    void repeatable_jobs_and_deduplication_not_supported() {}

    @Disabled("NOT IMPLEMENTED: BullMQ's exact state machine — waiting, active, delayed, prioritized, " +
              "completed, failed — does not map 1:1 to QueueFlux states. " +
              "Do not use QueueFlux outputs as direct predictions for BullMQ queue depths or state counts.")
    @Test
    void bullmq_exact_state_machine_semantics_not_matched() {}
}
