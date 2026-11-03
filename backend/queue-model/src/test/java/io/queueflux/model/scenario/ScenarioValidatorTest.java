package io.queueflux.model.scenario;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScenarioValidatorTest {
    private ScenarioDefinition validScenario(
            List<QueueDefinition> queues,
            List<WorkerPoolDefinition> pools,
            List<WorkloadDefinition> workloads) {
        return new ScenarioDefinition(1, "validator-test", 1_000_000, 42L, queues, pools, workloads);
    }

    private QueueDefinition queue(String id) {
        return new QueueDefinition(id, 10, SchedulingDiscipline.FIFO);
    }

    private WorkerPoolDefinition pool(String id, String queueId, List<WorkerOutage> outages) {
        return new WorkerPoolDefinition(id, queueId, 1, 1, ServiceTimeModel.constant(100), 0, outages);
    }

    private WorkloadDefinition workload(String id, String queueId, long endMicros) {
        return new WorkloadDefinition(id, queueId, 0, endMicros, ArrivalModel.constantRate(1));
    }

    @Test
    void acceptsAValidScenario() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("email-workers", "emails", List.of())),
                List.of(workload("email-load", "emails", 1_000_000)));

        assertThatCode(() -> ScenarioValidator.validate(scenario)).doesNotThrowAnyException();
    }

    @Test
    void rejectsDuplicateQueueIds() {
        var scenario = validScenario(
                List.of(queue("emails"), queue("emails")),
                List.of(pool("workers", "emails", List.of())),
                List.of(workload("load", "emails", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate queue id");
    }

    @Test
    void rejectsDuplicateWorkerPoolIds() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "emails", List.of()), pool("workers", "emails", List.of())),
                List.of(workload("load", "emails", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate worker pool id");
    }

    @Test
    void rejectsWorkerPoolsThatReferenceUnknownQueues() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "missing", List.of())),
                List.of(workload("load", "emails", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown queue for worker pool");
    }

    @Test
    void rejectsWorkloadsThatReferenceUnknownQueues() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "emails", List.of())),
                List.of(workload("load", "missing", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown queue for workload");
    }

    @Test
    void rejectsOutagesThatExceedSimulationDuration() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "emails", List.of(new WorkerOutage("maintenance", 100, 1_000_001, 1)))),
                List.of(workload("load", "emails", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("worker outage exceeds simulation duration");
    }

    @Test
    void rejectsWorkloadWindowsThatExceedSimulationDuration() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "emails", List.of())),
                List.of(workload("load", "emails", 1_000_001)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workload window exceeds simulation duration");
    }

    @Test
    void requiresExactlyOneWorkerPoolPerQueue() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers-a", "emails", List.of()), pool("workers-b", "emails", List.of())),
                List.of(workload("load", "emails", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exactly one worker pool per queue");
    }

    @Test
    void rejectsQueuesWithoutAWorkload() {
        var scenario = validScenario(
                List.of(queue("emails")),
                List.of(pool("workers", "emails", List.of())),
                List.of(workload("other-load", "other-queue", 1_000_000)));

        assertThatThrownBy(() -> ScenarioValidator.validate(scenario))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown queue for workload");
    }
}
