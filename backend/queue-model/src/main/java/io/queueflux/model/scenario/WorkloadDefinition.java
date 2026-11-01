package io.queueflux.model.scenario;

public record WorkloadDefinition(String id, String queueId, long startMicros, long endMicros, ArrivalModel arrivals) {
    public WorkloadDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("workload id is required");
        if (queueId == null || queueId.isBlank()) throw new IllegalArgumentException("queueId is required");
        if (startMicros < 0 || endMicros <= startMicros) throw new IllegalArgumentException("workload time window is invalid");
        if (arrivals == null) throw new IllegalArgumentException("arrivals is required");
    }
}
