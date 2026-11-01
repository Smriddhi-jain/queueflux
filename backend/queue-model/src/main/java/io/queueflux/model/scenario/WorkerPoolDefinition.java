package io.queueflux.model.scenario;

public record WorkerPoolDefinition(
        String id,
        String queueId,
        int workerCount,
        int concurrencyPerWorker,
        ServiceTimeModel serviceTime) {
    public WorkerPoolDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("worker pool id is required");
        if (queueId == null || queueId.isBlank()) throw new IllegalArgumentException("queueId is required");
        if (workerCount < 1) throw new IllegalArgumentException("workerCount must be >= 1");
        if (concurrencyPerWorker < 1) throw new IllegalArgumentException("concurrencyPerWorker must be >= 1");
        if (serviceTime == null) throw new IllegalArgumentException("serviceTime is required");
    }

    public int capacity() {
        return Math.multiplyExact(workerCount, concurrencyPerWorker);
    }
}
