package io.queueflux.engine;

import io.queueflux.engine.api.*;
import io.queueflux.engine.clock.VirtualClock;
import io.queueflux.engine.random.DeterministicRandom;
import io.queueflux.engine.scheduler.*;
import io.queueflux.engine.runtime.ServiceTimeSampler;
import io.queueflux.model.scenario.*;
import java.util.*;

/** Deterministic single-queue discrete-event engine. No wall-clock sleeps or external I/O. */
public final class DefaultSimulationEngine implements SimulationEngine {
    private enum JobStatus { WAITING, ACTIVE, COMPLETED, REJECTED }
    private static final class JobState {
        final String id;
        final long arrivalMicros;
        JobStatus status = JobStatus.WAITING;
        long startedMicros;
        JobState(String id, long arrivalMicros) { this.id = id; this.arrivalMicros = arrivalMicros; }
    }

    @Override
    public SimulationResult run(SimulationRequest request, SimulationObserver observer) {
        Objects.requireNonNull(request, "request");
        observer = observer == null ? SimulationObserver.noop() : observer;
        ScenarioDefinition scenario = request.scenario();
        ScenarioValidator.validate(scenario);

        QueueDefinition queue = scenario.queues().getFirst();
        WorkerPoolDefinition pool = scenario.workerPools().getFirst();
        if (scenario.queues().size() != 1 || scenario.workerPools().size() != 1) {
            throw new IllegalArgumentException("v0.1 engine supports exactly one queue and one worker pool");
        }
        if (!pool.queueId().equals(queue.id())) throw new IllegalArgumentException("worker pool must serve the configured queue");

        VirtualClock clock = new VirtualClock();
        EventScheduler scheduler = new EventScheduler();
        DeterministicRandom[] arrivalRandoms = new DeterministicRandom[scenario.workloads().size()];
        long[] nextJobNumber = new long[scenario.workloads().size()];
        for (int i = 0; i < scenario.workloads().size(); i++) {
            WorkloadDefinition workload = scenario.workloads().get(i);
            arrivalRandoms[i] = DeterministicRandom.stream(scenario.seed(), "arrival:" + workload.id());
            long first = workload.arrivals().mode() == ArrivalMode.CONSTANT
                    ? workload.startMicros()
                    : nextArrival(workload, workload.startMicros(), arrivalRandoms[i]);
            if (first < workload.endMicros() && first < scenario.durationMicros()) {
                scheduler.schedule(first, EventType.JOB_ARRIVAL, jobId(i, workload, nextJobNumber[i]++), queue.id());
            }
        }

        Deque<JobState> waiting = new ArrayDeque<>();
        Map<String, JobState> jobs = new HashMap<>();
        List<Long> queueWaits = new ArrayList<>();
        List<Long> latencies = new ArrayList<>();
        List<TraceEvent> trace = new ArrayList<>();
        int capacity = pool.capacity();
        int active = 0;
        long busySlotMicros = 0;
        long lastBusyUpdate = 0;
        long generated = 0, accepted = 0, completed = 0, rejected = 0, peakBacklog = 0;
        long processedEvents = 0;
        int maxTrace = request.maxTraceEvents();

        while (!scheduler.isEmpty()) {
            ScheduledEvent event = scheduler.poll();
            if (event.timeMicros() > scenario.durationMicros()) break;
            if (++processedEvents > request.maxEvents()) throw new IllegalStateException("maximum event limit exceeded");
            busySlotMicros += Math.max(0, event.timeMicros() - lastBusyUpdate) * active;
            lastBusyUpdate = event.timeMicros();
            clock.advanceTo(event.timeMicros());

            if (event.type() == EventType.JOB_ARRIVAL) {
                if (generated >= request.maxGeneratedJobs()) throw new IllegalStateException("maximum generated job limit exceeded");
                int workloadIndex = workloadIndex(scenario.workloads(), event.jobId());
                WorkloadDefinition workload = scenario.workloads().get(workloadIndex);
                generated++;
                JobState job = new JobState(event.jobId(), event.timeMicros());
                if (waiting.size() >= queue.maxDepth()) {
                    job.status = JobStatus.REJECTED;
                    rejected++;
                    addTrace(trace, maxTrace, new TraceEvent(clock.nowMicros(), "JOB_REJECTED", job.id, "Queue reached maxDepth"));
                } else {
                    jobs.put(job.id, job);
                    waiting.addLast(job);
                    accepted++;
                    addTrace(trace, maxTrace, new TraceEvent(clock.nowMicros(), "JOB_ARRIVED", job.id, "Accepted into queue"));
                    active = dispatch(waiting, scheduler, pool, clock.nowMicros(), active, capacity, trace, maxTrace, scenario.seed());
                    peakBacklog = Math.max(peakBacklog, waiting.size());
                }

                long next = nextArrival(workload, event.timeMicros(), arrivalRandoms[workloadIndex]);
                if (next > event.timeMicros() && next < workload.endMicros() && next < scenario.durationMicros()) {
                    scheduler.schedule(next, EventType.JOB_ARRIVAL, jobId(workloadIndex, workload, nextJobNumber[workloadIndex]++), queue.id());
                }
            } else if (event.type() == EventType.JOB_COMPLETED) {
                JobState job = jobs.get(event.jobId());
                if (job != null && job.status == JobStatus.ACTIVE) {
                    job.status = JobStatus.COMPLETED;
                    jobs.remove(job.id);
                    active--;
                    completed++;
                    queueWaits.add(job.startedMicros - job.arrivalMicros);
                    latencies.add(clock.nowMicros() - job.arrivalMicros);
                    addTrace(trace, maxTrace, new TraceEvent(clock.nowMicros(), "JOB_COMPLETED", job.id, "Processing completed"));
                    active = dispatch(waiting, scheduler, pool, clock.nowMicros(), active, capacity, trace, maxTrace, scenario.seed());
                }
            }

            if (processedEvents % 10_000 == 0) observer.onProgress(new SimulationProgress(clock.nowMicros(), processedEvents, processedEvents + scheduler.size()));
        }

        busySlotMicros += Math.max(0, scenario.durationMicros() - lastBusyUpdate) * active;
        long activeAtEnd = active;
        double seconds = scenario.durationMicros() / 1_000_000d;
        double throughput = seconds == 0 ? 0 : completed / seconds;
        double utilization = seconds == 0 ? 0 : Math.min(1d, busySlotMicros / (double) (capacity * scenario.durationMicros()));
        SimulationResult result = new SimulationResult(
                scenario.name(), scenario.seed(), scenario.durationMicros(), processedEvents,
                generated, accepted, completed, rejected, waiting.size(), activeAtEnd, peakBacklog,
                throughput, mean(queueWaits), mean(latencies), percentile(latencies, 0.95), percentile(latencies, 0.99),
                utilization, trace);
        observer.onProgress(new SimulationProgress(scenario.durationMicros(), processedEvents, processedEvents));
        return result;
    }

    private static int dispatch(Deque<JobState> waiting, EventScheduler scheduler,
            WorkerPoolDefinition pool, long now, int active, int capacity, List<TraceEvent> trace, int maxTrace, long seed) {
        int activeNow = active;
        while (activeNow < capacity && !waiting.isEmpty()) {
            JobState job = waiting.removeFirst();
            job.status = JobStatus.ACTIVE;
            job.startedMicros = now;
            long service = ServiceTimeSampler.sample(pool.serviceTime(), seed, job.id);
            scheduler.schedule(Math.addExact(now, service), EventType.JOB_COMPLETED, job.id, pool.queueId());
            addTrace(trace, maxTrace, new TraceEvent(now, "JOB_STARTED", job.id, "Service time: " + service + " µs"));
            activeNow++;
        }
        return activeNow;
    }

    private static long nextArrival(WorkloadDefinition workload, long current, DeterministicRandom random) {
        long gap;
        if (workload.arrivals().mode() == ArrivalMode.CONSTANT) {
            gap = Math.max(1L, Math.round(1_000_000d / workload.arrivals().ratePerSecond()));
        } else {
            double u = Math.max(Double.MIN_VALUE, 1d - random.nextDouble());
            gap = Math.max(1L, (long) Math.ceil(-Math.log(u) * 1_000_000d / workload.arrivals().ratePerSecond()));
        }
        return Math.addExact(current, gap);
    }

    private static String jobId(int workloadIndex, WorkloadDefinition workload, long jobNumber) {
        return "w" + workloadIndex + ":" + workload.id() + ":" + jobNumber;
    }

    private static int workloadIndex(List<WorkloadDefinition> workloads, String jobId) {
        int separator = jobId.indexOf(':');
        if (separator < 2 || jobId.charAt(0) != 'w') {
            throw new IllegalStateException("invalid job id " + jobId);
        }
        try {
            int index = Integer.parseInt(jobId.substring(1, separator));
            if (index < 0 || index >= workloads.size()) throw new NumberFormatException("out of range");
            return index;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("workload not found for job " + jobId, exception);
        }
    }

    private static void addTrace(List<TraceEvent> trace, int maxTrace, TraceEvent event) {
        if (trace.size() < maxTrace) trace.add(event);
    }

    private static double mean(List<Long> values) {
        return values.isEmpty() ? 0 : values.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private static double percentile(List<Long> values, double percentile) {
        if (values.isEmpty()) return 0;
        long[] sorted = values.stream().mapToLong(Long::longValue).sorted().toArray();
        int index = Math.max(0, (int) Math.ceil(percentile * sorted.length) - 1);
        return sorted[index];
    }
}
