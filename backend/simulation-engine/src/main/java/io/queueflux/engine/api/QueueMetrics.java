package io.queueflux.engine.api;

public record QueueMetrics(
        String queueId,
        long acceptedJobs,
        long completedJobs,
        long rejectedJobs,
        long retriedJobs,
        long deadLetteredJobs,
        long waitingAtEnd,
        long delayedRetryAtEnd,
        long activeAtEnd,
        long peakBacklog,
        double throughputPerSecond,
        double averageQueueWaitMicros,
        double p95LatencyMicros,
        double p99LatencyMicros,
        double workerUtilization) {}
