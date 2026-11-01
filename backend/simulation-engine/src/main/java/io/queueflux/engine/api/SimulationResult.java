package io.queueflux.engine.api;

import java.util.List;

public record SimulationResult(
        String scenarioName,
        long seed,
        long simulationDurationMicros,
        long processedEvents,
        long generatedJobs,
        long acceptedJobs,
        long completedJobs,
        long rejectedJobs,
        long waitingAtEnd,
        long activeAtEnd,
        long peakBacklog,
        double throughputPerSecond,
        double averageQueueWaitMicros,
        double averageEndToEndLatencyMicros,
        double p95EndToEndLatencyMicros,
        double p99EndToEndLatencyMicros,
        double workerUtilization,
        List<TraceEvent> trace) {
    public SimulationResult {
        trace = List.copyOf(trace);
    }
}
