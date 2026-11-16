package io.queueflux.engine;

import io.queueflux.engine.api.*;
import io.queueflux.model.scenario.*;
import net.jqwik.api.*;
import org.assertj.core.api.Assertions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Property-based tests for the simulation engine. jqwik generates hundreds of random
 * scenario combinations and verifies that core invariants hold for all of them. When a
 * property fails, jqwik reports the seed needed to reproduce the exact failing scenario.
 */
class SimulationEnginePropertyTest {

    // ── Determinism ────────────────────────────────────────────────────────────

    @Property(tries = 200)
    void sameSeedAlwaysProducesIdenticalResults(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        var engine = new DefaultSimulationEngine();
        assertThat(engine.run(SimulationRequest.defaults(scenario)))
                .isEqualTo(engine.run(SimulationRequest.defaults(scenario)));
    }

    // ── Job accounting ─────────────────────────────────────────────────────────

    @Property(tries = 300)
    void generationAccountingIsExact(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        // Every generated job is either accepted into a queue or rejected immediately.
        assertThat(r.acceptedJobs() + r.rejectedJobs()).isEqualTo(r.generatedJobs());
    }

    @Property(tries = 300)
    void acceptedJobsAreFullyAccountedFor(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        // Every accepted job ends up in exactly one terminal or in-progress bucket.
        long accounted = r.completedJobs() + r.deadLetteredJobs()
                + r.waitingAtEnd() + r.activeAtEnd() + r.delayedRetryAtEnd();
        assertThat(accounted).isEqualTo(r.acceptedJobs());
    }

    @Property(tries = 200)
    void failedAttemptsAreAtLeastEqualToDeadLetteredJobs(@ForAll("retriableSingleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        // Each dead-lettered job required at least one failed processing attempt.
        assertThat(r.failedAttempts()).isGreaterThanOrEqualTo(r.deadLetteredJobs());
    }

    // ── Bounded queue ──────────────────────────────────────────────────────────

    @Property(tries = 300)
    void peakBacklogNeverExceedsQueueMaxDepth(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        // The engine rejects arriving jobs when the waiting queue is full; backlog cannot exceed maxDepth.
        assertThat(r.peakBacklog()).isLessThanOrEqualTo(scenario.queues().get(0).maxDepth());
    }

    // ── Metric bounds ──────────────────────────────────────────────────────────

    @Property(tries = 300)
    void workerUtilizationIsBoundedBetweenZeroAndOne(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        assertThat(r.workerUtilization()).isBetween(0.0, 1.0);
    }

    @Property(tries = 300)
    void allCountsAreNonNegative(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        Assertions.assertThat(r.generatedJobs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.acceptedJobs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.completedJobs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.rejectedJobs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.deadLetteredJobs()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.peakBacklog()).isGreaterThanOrEqualTo(0);
        Assertions.assertThat(r.throughputPerSecond()).isGreaterThanOrEqualTo(0.0);
    }

    @Property(tries = 300)
    void latencyPercentilesAreOrdered(@ForAll("singleQueueScenarios") ScenarioDefinition scenario) {
        SimulationResult r = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario));
        if (r.completedJobs() > 0) {
            assertThat(r.p95EndToEndLatencyMicros()).isLessThanOrEqualTo(r.p99EndToEndLatencyMicros());
            assertThat(r.averageEndToEndLatencyMicros()).isGreaterThanOrEqualTo(0.0);
        }
    }

    // ── Scenario providers ─────────────────────────────────────────────────────

    @Provide
    Arbitrary<ScenarioDefinition> singleQueueScenarios() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, Long.MAX_VALUE / 2),
                Arbitraries.longs().between(1_000_000L, 30_000_000L),
                Arbitraries.integers().between(1, 200),
                Arbitraries.integers().between(1, 8),
                Arbitraries.longs().between(1_000L, 500_000L),
                Arbitraries.doubles().between(0.5, 40.0)
        ).as((seed, duration, maxDepth, workers, serviceTime, arrivalRate) ->
                new ScenarioDefinition(1, "prop-test", duration, seed,
                        List.of(new QueueDefinition("q", maxDepth, SchedulingDiscipline.FIFO)),
                        List.of(new WorkerPoolDefinition("pool", "q", workers, 1,
                                ServiceTimeModel.constant(serviceTime))),
                        List.of(new WorkloadDefinition("load", "q", 0, duration,
                                ArrivalModel.constantRate(arrivalRate)))
                ));
    }

    @Provide
    Arbitrary<ScenarioDefinition> retriableSingleQueueScenarios() {
        return Combinators.combine(
                Arbitraries.longs().between(1L, Long.MAX_VALUE / 2),
                Arbitraries.longs().between(5_000_000L, 20_000_000L),
                Arbitraries.integers().between(5, 100),
                Arbitraries.integers().between(1, 4),
                Arbitraries.longs().between(5_000L, 50_000L),
                Arbitraries.doubles().between(1.0, 10.0),
                Arbitraries.integers().between(2, 5),
                Arbitraries.doubles().between(0.1, 0.9)
        ).as((seed, duration, maxDepth, workers, serviceTime, arrivalRate, maxAttempts, failProb) ->
                new ScenarioDefinition(1, "prop-retry", duration, seed,
                        List.of(new QueueDefinition("q", maxDepth, SchedulingDiscipline.FIFO,
                                new RetryPolicy(maxAttempts, 10_000L, 100_000L, 2.0, 0.0),
                                RateLimitPolicy.unlimited())),
                        List.of(new WorkerPoolDefinition("pool", "q", workers, 1,
                                ServiceTimeModel.constant(serviceTime), failProb, List.of())),
                        List.of(new WorkloadDefinition("load", "q", 0, duration,
                                ArrivalModel.constantRate(arrivalRate)))
                ));
    }
}
