package io.queueflux.model.scenario;

import java.util.HashSet;
import java.util.Set;

public final class ScenarioValidator {
    private ScenarioValidator() {}

    public static void validate(ScenarioDefinition scenario) {
        Set<String> queueIds = new HashSet<>();
        scenario.queues().forEach(queue -> {
            if (!queueIds.add(queue.id())) throw new IllegalArgumentException("duplicate queue id: " + queue.id());
            if (queue.discipline() != SchedulingDiscipline.FIFO) {
                throw new IllegalArgumentException("only FIFO scheduling is supported in v0.1");
            }
        });
        Set<String> poolIds = new HashSet<>();
        scenario.workerPools().forEach(pool -> {
            if (!poolIds.add(pool.id())) throw new IllegalArgumentException("duplicate worker pool id: " + pool.id());
            if (!queueIds.contains(pool.queueId())) throw new IllegalArgumentException("unknown queue for worker pool: " + pool.queueId());
        });
        Set<String> workloadIds = new HashSet<>();
        scenario.workloads().forEach(workload -> {
            if (!workloadIds.add(workload.id())) throw new IllegalArgumentException("duplicate workload id: " + workload.id());
            if (!queueIds.contains(workload.queueId())) throw new IllegalArgumentException("unknown queue for workload: " + workload.queueId());
            if (workload.endMicros() > scenario.durationMicros()) throw new IllegalArgumentException("workload window exceeds simulation duration");
        });
        for (QueueDefinition queue : scenario.queues()) {
            long pools = scenario.workerPools().stream().filter(pool -> pool.queueId().equals(queue.id())).count();
            if (pools != 1) throw new IllegalArgumentException("exactly one worker pool per queue is currently supported: " + queue.id());
        }
    }
}
