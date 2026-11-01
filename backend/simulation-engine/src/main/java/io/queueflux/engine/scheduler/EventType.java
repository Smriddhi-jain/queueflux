package io.queueflux.engine.scheduler;

public enum EventType {
    JOB_COMPLETED(0),
    WORKER_OUTAGE_END(1),
    WORKER_OUTAGE_START(2),
    JOB_ARRIVAL(3),
    JOB_RETRY_READY(4),
    QUEUE_DISPATCH(5);

    private final int phase;
    EventType(int phase) { this.phase = phase; }
    public int phase() { return phase; }
}
