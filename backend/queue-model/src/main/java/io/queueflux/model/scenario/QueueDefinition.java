package io.queueflux.model.scenario;

public record QueueDefinition(String id, int maxDepth, SchedulingDiscipline discipline) {
    public QueueDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("queue id is required");
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be >= 1");
        if (discipline == null) throw new IllegalArgumentException("discipline is required");
    }
}
