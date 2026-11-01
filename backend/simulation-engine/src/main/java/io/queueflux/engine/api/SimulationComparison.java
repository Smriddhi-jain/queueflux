package io.queueflux.engine.api;

import java.util.List;

public record SimulationComparison(
        int replications,
        long seedStride,
        double meanThroughputPerSecond,
        double standardDeviationThroughputPerSecond,
        double meanP95LatencyMicros,
        double standardDeviationP95LatencyMicros,
        double meanP99LatencyMicros,
        double meanPeakBacklog,
        double meanRejectedJobs,
        double meanWorkerUtilization,
        List<ReplicationSummary> runs) {
    public SimulationComparison { runs = List.copyOf(runs); }

    public record ReplicationSummary(long seed, double throughputPerSecond, double p95LatencyMicros,
                                     double p99LatencyMicros, long peakBacklog, long rejectedJobs,
                                     double workerUtilization) {}
}
