package io.queueflux.model.scenario;

/** A planned interval during which the specified number of workers stop accepting new jobs. */
public record WorkerOutage(String id, long startMicros, long endMicros, int unavailableWorkers) {
    public WorkerOutage {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("outage id is required");
        if (startMicros < 0 || endMicros <= startMicros) throw new IllegalArgumentException("outage interval is invalid");
        if (unavailableWorkers < 1) throw new IllegalArgumentException("unavailableWorkers must be >= 1");
    }
}
