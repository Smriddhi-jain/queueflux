package io.queueflux.model.scenario;

import java.util.List;

public record WorkerPoolDefinition(
        String id,
        String queueId,
        int workerCount,
        int concurrencyPerWorker,
        ServiceTimeModel serviceTime,
        double failureProbability,
        List<WorkerOutage> outages) {
    public WorkerPoolDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("worker pool id is required");
        if (queueId == null || queueId.isBlank()) throw new IllegalArgumentException("queueId is required");
        if (workerCount < 1) throw new IllegalArgumentException("workerCount must be >= 1");
        if (concurrencyPerWorker < 1) throw new IllegalArgumentException("concurrencyPerWorker must be >= 1");
        if (serviceTime == null) throw new IllegalArgumentException("serviceTime is required");
        if (!Double.isFinite(failureProbability) || failureProbability < 0 || failureProbability > 1) {
            throw new IllegalArgumentException("failureProbability must be between 0 and 1");
        }
        outages = List.copyOf(outages == null ? List.of() : outages);
        for (WorkerOutage outage : outages) {
            if (outage.unavailableWorkers() > workerCount) {
                throw new IllegalArgumentException("an outage cannot remove more workers than the pool contains");
            }
        }
    }

    public WorkerPoolDefinition(String id, String queueId, int workerCount, int concurrencyPerWorker, ServiceTimeModel serviceTime) {
        this(id, queueId, workerCount, concurrencyPerWorker, serviceTime, 0, List.of());
    }

    public int capacity() {
        return Math.multiplyExact(workerCount, concurrencyPerWorker);
    }
}
