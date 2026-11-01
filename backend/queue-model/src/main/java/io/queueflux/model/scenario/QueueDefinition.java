package io.queueflux.model.scenario;

public record QueueDefinition(String id, int maxDepth, SchedulingDiscipline discipline,
                              RetryPolicy retryPolicy, RateLimitPolicy rateLimit) {
    public QueueDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("queue id is required");
        if (maxDepth < 1) throw new IllegalArgumentException("maxDepth must be >= 1");
        if (discipline == null) throw new IllegalArgumentException("discipline is required");
        retryPolicy = retryPolicy == null ? RetryPolicy.disabled() : retryPolicy;
        rateLimit = rateLimit == null ? RateLimitPolicy.unlimited() : rateLimit;
    }

    public QueueDefinition(String id, int maxDepth, SchedulingDiscipline discipline) {
        this(id, maxDepth, discipline, RetryPolicy.disabled(), RateLimitPolicy.unlimited());
    }
}
