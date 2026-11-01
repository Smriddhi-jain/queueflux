package io.queueflux.engine;

import io.queueflux.engine.api.SimulationRequest;
import io.queueflux.engine.api.SimulationResult;
import io.queueflux.model.scenario.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DefaultSimulationEngineTest {
    private ScenarioDefinition scenario() {
        return new ScenarioDefinition(1, "deterministic-smoke", 10_000_000, 12345L,
                java.util.List.of(new QueueDefinition("emails", 100, SchedulingDiscipline.FIFO)),
                java.util.List.of(new WorkerPoolDefinition("workers", "emails", 2, 1, ServiceTimeModel.constant(200_000))),
                java.util.List.of(new WorkloadDefinition("arrivals", "emails", 0, 10_000_000, ArrivalModel.constantRate(5))));
    }

    @Test
    void sameSeedProducesSameResultsAndTrace() {
        var engine = new DefaultSimulationEngine();
        var first = engine.run(SimulationRequest.defaults(scenario()));
        var second = engine.run(SimulationRequest.defaults(scenario()));
        assertThat(first).isEqualTo(second);
    }

    @Test
    void stableWorkloadCompletesWithoutRejectionWhenCapacityIsSufficient() {
        SimulationResult result = new DefaultSimulationEngine().run(SimulationRequest.defaults(scenario()));
        assertThat(result.generatedJobs()).isEqualTo(50);
        assertThat(result.completedJobs()).isEqualTo(50);
        assertThat(result.rejectedJobs()).isZero();
        assertThat(result.peakBacklog()).isZero();
        assertThat(result.workerUtilization()).isBetween(0.0, 1.0);
    }

    @Test
    void poissonAndExponentialWorkloadIsRepeatableForTheSameSeed() {
        var base = scenario();
        var stochastic = new ScenarioDefinition(1, "stochastic", base.durationMicros(), 9876L,
                base.queues(),
                java.util.List.of(new WorkerPoolDefinition("workers", "emails", 2, 2, ServiceTimeModel.exponential(180_000))),
                java.util.List.of(new WorkloadDefinition("arrivals", "emails", 0, base.durationMicros(), ArrivalModel.poisson(18))));
        var engine = new DefaultSimulationEngine();
        var first = engine.run(SimulationRequest.defaults(stochastic));
        var second = engine.run(SimulationRequest.defaults(stochastic));
        assertThat(first).isEqualTo(second);
        assertThat(first.generatedJobs()).isPositive();
    }

    @Test
    void traceLimitDoesNotChangeSimulationMetrics() {
        var scenario = scenario();
        var engine = new DefaultSimulationEngine();
        var fullTrace = engine.run(SimulationRequest.defaults(scenario));
        var noTrace = engine.run(new SimulationRequest(scenario, 100_000, 100_000, 0));
        assertThat(noTrace.trace()).isEmpty();
        assertThat(noTrace.completedJobs()).isEqualTo(fullTrace.completedJobs());
        assertThat(noTrace.workerUtilization()).isEqualTo(fullTrace.workerUtilization());
    }

    @Test
    void queueBacklogIsBoundedAndExcessArrivalsAreRejected() {
        var overloaded = new ScenarioDefinition(1, "overloaded", 2_000_000, 7L,
                java.util.List.of(new QueueDefinition("emails", 1, SchedulingDiscipline.FIFO)),
                java.util.List.of(new WorkerPoolDefinition("workers", "emails", 1, 1, ServiceTimeModel.constant(1_000_000))),
                java.util.List.of(new WorkloadDefinition("arrivals", "emails", 0, 2_000_000, ArrivalModel.constantRate(10))));
        var result = new DefaultSimulationEngine().run(SimulationRequest.defaults(overloaded));
        assertThat(result.peakBacklog()).isEqualTo(1);
        assertThat(result.rejectedJobs()).isPositive();
    }

    @Test
    void validatesQueueReferences() {
        var invalid = new ScenarioDefinition(1, "bad", 1_000_000, 1,
                java.util.List.of(new QueueDefinition("q", 1, SchedulingDiscipline.FIFO)),
                java.util.List.of(new WorkerPoolDefinition("pool", "missing", 1, 1, ServiceTimeModel.constant(10))),
                java.util.List.of(new WorkloadDefinition("w", "q", 0, 1_000_000, ArrivalModel.constantRate(1))));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new DefaultSimulationEngine().run(SimulationRequest.defaults(invalid)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("unknown queue");
    }
}
