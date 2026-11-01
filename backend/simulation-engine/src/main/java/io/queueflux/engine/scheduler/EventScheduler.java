package io.queueflux.engine.scheduler;

import java.util.PriorityQueue;

public final class EventScheduler {
    private final PriorityQueue<ScheduledEvent> events = new PriorityQueue<>();
    private long nextSequence;

    public void schedule(long timeMicros, EventType type, String jobId, String queueId) {
        schedule(timeMicros, type, jobId, queueId, null);
    }

    public void schedule(long timeMicros, EventType type, String jobId, String queueId, String outageId) {
        if (timeMicros < 0) throw new IllegalArgumentException("event time cannot be negative");
        events.add(new ScheduledEvent(timeMicros, nextSequence++, type, jobId, queueId, outageId));
    }

    public ScheduledEvent poll() { return events.poll(); }
    public int size() { return events.size(); }
    public boolean isEmpty() { return events.isEmpty(); }
}
