package io.queueflux.engine;

import io.queueflux.engine.api.*;
import io.queueflux.model.scenario.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultSimulationEngineTest {
    private ScenarioDefinition scenario() {
        return new ScenarioDefinition(1, "deterministic-smoke", 10_000_000, 12345L,
                List.of(new QueueDefinition("emails", 100, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("workers", "emails", 2, 1, ServiceTimeModel.constant(200_000))),
                List.of(new WorkloadDefinition("arrivals", "emails", 0, 10_000_000, ArrivalModel.constantRate(5))));
    }

    @Test void sameSeedProducesSameResultsAndTrace() {
        var engine = new DefaultSimulationEngine();
        assertThat(engine.run(SimulationRequest.defaults(scenario())))
                .isEqualTo(engine.run(SimulationRequest.defaults(scenario())));
    }

    @Test void stableWorkloadCompletesWhenCapacityIsSufficient() {
        SimulationResult result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario()));
        assertThat(result.generatedJobs()).isEqualTo(50);
        assertThat(result.completedJobs()).isEqualTo(50);
        assertThat(result.rejectedJobs()).isZero();
        assertThat(result.peakBacklog()).isZero();
        assertThat(result.workerUtilization()).isBetween(0.45, 0.55);
    }

    @Test void poissonAndExponentialWorkloadIsRepeatable() {
        var base = scenario();
        var stochastic = new ScenarioDefinition(1, "stochastic", base.durationMicros(), 9876L, base.queues(),
                List.of(new WorkerPoolDefinition("workers", "emails", 2, 2, ServiceTimeModel.exponential(180_000))),
                List.of(new WorkloadDefinition("arrivals", "emails", 0, base.durationMicros(), ArrivalModel.poisson(18))));
        var engine = new DefaultSimulationEngine();
        assertThat(engine.run(SimulationRequest.defaults(stochastic))).isEqualTo(engine.run(SimulationRequest.defaults(stochastic)));
    }

    @Test void traceLimitDoesNotChangeMetrics() {
        var engine = new DefaultSimulationEngine();
        var full = engine.run(SimulationRequest.defaults(scenario()));
        var quiet = engine.run(new SimulationRequest(scenario(), 100_000, 100_000, 0));
        assertThat(quiet.trace()).isEmpty();
        assertThat(quiet.completedJobs()).isEqualTo(full.completedJobs());
        assertThat(quiet.workerUtilization()).isEqualTo(full.workerUtilization());
    }

    @Test void queueBacklogIsBoundedAndExcessArrivalsAreRejected() {
        var overloaded = new ScenarioDefinition(1, "overloaded", 2_000_000, 7L,
                List.of(new QueueDefinition("emails", 1, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("workers", "emails", 1, 1, ServiceTimeModel.constant(1_000_000))),
                List.of(new WorkloadDefinition("arrivals", "emails", 0, 2_000_000, ArrivalModel.constantRate(10))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(overloaded));
        assertThat(result.peakBacklog()).isEqualTo(1);
        assertThat(result.rejectedJobs()).isPositive();
    }

    @Test void supportsMultipleIndependentQueues() {
        var multi = new ScenarioDefinition(1, "multi-queue", 2_000_000, 12L,
                List.of(new QueueDefinition("mail", 10, SchedulingDiscipline.FIFO),
                        new QueueDefinition("reports", 10, SchedulingDiscipline.PRIORITY)),
                List.of(new WorkerPoolDefinition("mail-workers", "mail", 1, 1, ServiceTimeModel.constant(100_000)),
                        new WorkerPoolDefinition("report-workers", "reports", 2, 1, ServiceTimeModel.constant(200_000))),
                List.of(new WorkloadDefinition("mail-load", "mail", 0, 2_000_000, ArrivalModel.constantRate(5)),
                        new WorkloadDefinition("report-load", "reports", 0, 2_000_000, ArrivalModel.constantRate(3))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(multi));
        assertThat(result.queues()).hasSize(2);
        assertThat(result.completedJobs()).isGreaterThan(0);
        assertThat(result.queues()).extracting(QueueMetrics::queueId).containsExactly("mail", "reports");
    }

    @Test void globalTokenBucketLimitsJobStarts() {
        var limited = new ScenarioDefinition(1, "limited", 2_000_000, 42L,
                List.of(new QueueDefinition("q", 20, SchedulingDiscipline.FIFO, RetryPolicy.disabled(), new RateLimitPolicy(2, 1))),
                List.of(new WorkerPoolDefinition("pool", "q", 5, 1, ServiceTimeModel.constant(10_000))),
                List.of(new WorkloadDefinition("load", "q", 0, 2_000_000, ArrivalModel.constantRate(10))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(limited));
        var startedAt = result.trace().stream().filter(e -> e.type().equals("JOB_STARTED")).map(TraceEvent::timeMicros).toList();
        assertThat(startedAt).isNotEmpty();
        for (int i = 1; i < startedAt.size(); i++) assertThat(startedAt.get(i) - startedAt.get(i-1)).isGreaterThanOrEqualTo(500_000);
    }

    @Test void retriesFailuresAndMovesExhaustedJobsToDeadLetterOutcome() {
        var retry = new ScenarioDefinition(1, "retry", 2_000_000, 7L,
                List.of(new QueueDefinition("q", 20, SchedulingDiscipline.FIFO,
                        new RetryPolicy(2, 1_000, 10_000, 2, 0), RateLimitPolicy.unlimited())),
                List.of(new WorkerPoolDefinition("pool", "q", 1, 1, ServiceTimeModel.constant(10_000), 1.0, List.of())),
                List.of(new WorkloadDefinition("load", "q", 0, 1_000_000, ArrivalModel.constantRate(5))));
        var engine = new DefaultSimulationEngine();
        var result = engine.run(SimulationRequest.defaults(retry));
        assertThat(result.retriedJobs()).isEqualTo(5);
        assertThat(result.failedAttempts()).isEqualTo(10);
        assertThat(result.deadLetteredJobs()).isEqualTo(5);
        assertThat(result.completedJobs()).isZero();
    }

    @Test void workerOutagePausesNewDispatchUntilCapacityReturns() {
        var outage = new ScenarioDefinition(1, "outage", 2_000_000, 9L,
                List.of(new QueueDefinition("q", 20, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("pool", "q", 2, 1, ServiceTimeModel.constant(10_000), 0,
                        List.of(new WorkerOutage("maintenance", 0, 1_000_000, 2)))),
                List.of(new WorkloadDefinition("load", "q", 0, 1_000_000, ArrivalModel.constantRate(1))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(outage));
        assertThat(result.trace().stream().filter(e -> e.type().equals("JOB_STARTED")).map(TraceEvent::timeMicros))
                .allMatch(time -> time >= 1_000_000);
    }

    @Test void rejectsUnknownQueueReferences() {
        var invalid = new ScenarioDefinition(1, "bad", 1_000_000, 1,
                List.of(new QueueDefinition("q", 1, SchedulingDiscipline.FIFO)),
                List.of(new WorkerPoolDefinition("pool", "missing", 1, 1, ServiceTimeModel.constant(10))),
                List.of(new WorkloadDefinition("w", "q", 0, 1_000_000, ArrivalModel.constantRate(1))));
        assertThatThrownBy(() -> new DefaultSimulationEngine().run(SimulationRequest.defaults(invalid)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown queue");
    }
    @Test void priorityQueueStartsHigherPriorityWorkloadFirst() {
        var priority = new ScenarioDefinition(1, "priority", 2_000_000, 22L,
                List.of(new QueueDefinition("q", 10, SchedulingDiscipline.PRIORITY)),
                List.of(new WorkerPoolDefinition("pool", "q", 1, 1, ServiceTimeModel.constant(1_000_000))),
                List.of(new WorkloadDefinition("prefill", "q", 0, 100_000, ArrivalModel.constantRate(1), 0),
                        new WorkloadDefinition("urgent", "q", 100_000, 200_000, ArrivalModel.constantRate(1), 10),
                        new WorkloadDefinition("normal", "q", 200_000, 300_000, ArrivalModel.constantRate(1), 1)));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(priority));
        var started = result.trace().stream().filter(event -> event.type().equals("JOB_STARTED"))
                .map(TraceEvent::jobId).toList();
        assertThat(started).containsExactly("w0:prefill:0", "w1:urgent:0", "w2:normal:0");
    }

}
