package io.queueflux.engine.scheduler;

public enum EventType {
    JOB_COMPLETED(0),
    JOB_ARRIVAL(1);

    private final int phase;
    EventType(int phase) { this.phase = phase; }
    public int phase() { return phase; }
}
