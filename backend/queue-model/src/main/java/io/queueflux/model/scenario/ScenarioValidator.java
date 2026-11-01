package io.queueflux.model.scenario;

import java.util.HashSet;
import java.util.Set;

public final class ScenarioValidator {
    private ScenarioValidator() {}

    public static void validate(ScenarioDefinition scenario) {
        if (scenario == null) throw new IllegalArgumentException("scenario is required");
        Set<String> queueIds = new HashSet<>();
        for (QueueDefinition queue : scenario.queues()) {
            if (!queueIds.add(queue.id())) throw new IllegalArgumentException("duplicate queue id: " + queue.id());
        }

        Set<String> poolIds = new HashSet<>();
        for (WorkerPoolDefinition pool : scenario.workerPools()) {
            if (!poolIds.add(pool.id())) throw new IllegalArgumentException("duplicate worker pool id: " + pool.id());
            if (!queueIds.contains(pool.queueId())) throw new IllegalArgumentException("unknown queue for worker pool: " + pool.queueId());
            Set<String> outageIds = new HashSet<>();
            for (WorkerOutage outage : pool.outages()) {
                if (!outageIds.add(outage.id())) throw new IllegalArgumentException("duplicate worker outage id: " + outage.id());
                if (outage.endMicros() > scenario.durationMicros()) throw new IllegalArgumentException("worker outage exceeds simulation duration");
            }
        }

        Set<String> workloadIds = new HashSet<>();
        for (WorkloadDefinition workload : scenario.workloads()) {
            if (!workloadIds.add(workload.id())) throw new IllegalArgumentException("duplicate workload id: " + workload.id());
            if (!queueIds.contains(workload.queueId())) throw new IllegalArgumentException("unknown queue for workload: " + workload.queueId());
            if (workload.endMicros() > scenario.durationMicros()) throw new IllegalArgumentException("workload window exceeds simulation duration");
        }

        for (QueueDefinition queue : scenario.queues()) {
            long pools = scenario.workerPools().stream().filter(pool -> pool.queueId().equals(queue.id())).count();
            if (pools != 1) throw new IllegalArgumentException("exactly one worker pool per queue is currently supported: " + queue.id());
            if (scenario.workloads().stream().noneMatch(workload -> workload.queueId().equals(queue.id()))) {
                throw new IllegalArgumentException("queue has no workload: " + queue.id());
            }
        }
    }
}
