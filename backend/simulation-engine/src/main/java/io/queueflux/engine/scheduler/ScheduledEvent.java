package io.queueflux.engine.scheduler;

public record ScheduledEvent(long timeMicros, long sequence, EventType type,
                             String jobId, String queueId, String outageId) implements Comparable<ScheduledEvent> {
    @Override
    public int compareTo(ScheduledEvent other) {
        int byTime = Long.compare(timeMicros, other.timeMicros);
        if (byTime != 0) return byTime;
        int byPhase = Integer.compare(type.phase(), other.type.phase());
        return byPhase != 0 ? byPhase : Long.compare(sequence, other.sequence);
    }
}
