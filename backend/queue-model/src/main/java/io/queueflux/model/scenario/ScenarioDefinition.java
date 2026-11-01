package io.queueflux.model.scenario;

import java.util.List;

public record ScenarioDefinition(
        int schemaVersion,
        String name,
        long durationMicros,
        long seed,
        List<QueueDefinition> queues,
        List<WorkerPoolDefinition> workerPools,
        List<WorkloadDefinition> workloads) {
    public ScenarioDefinition {
        if (schemaVersion != 1) throw new IllegalArgumentException("only scenario schemaVersion 1 is supported");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("scenario name is required");
        if (durationMicros <= 0) throw new IllegalArgumentException("durationMicros must be > 0");
        queues = List.copyOf(queues == null ? List.of() : queues);
        workerPools = List.copyOf(workerPools == null ? List.of() : workerPools);
        workloads = List.copyOf(workloads == null ? List.of() : workloads);
        if (queues.isEmpty() || workerPools.isEmpty() || workloads.isEmpty()) {
            throw new IllegalArgumentException("scenario requires at least one queue, worker pool, and workload");
        }
    }
}
