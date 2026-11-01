# Metrics and Definitions

All time metrics are microseconds. Convert to milliseconds by dividing by 1,000 and seconds by dividing by 1,000,000.

## Scenario-level metrics

| Metric | Definition |
|---|---|
| Generated jobs | New arrival events processed within the simulation horizon |
| Accepted jobs | New jobs admitted into a queue; retries do not increment this |
| Completed jobs | Jobs that reach a successful completion event by the horizon |
| Rejected jobs | New arrivals not admitted because waiting depth is full |
| Retried jobs | Number of retry schedules created after transient failures |
| Failed attempts | Number of service attempts that fail |
| Dead-lettered jobs | Jobs that exhaust attempts or cannot re-enter a full waiting queue |
| Waiting at end | Jobs in queue waiting for worker capacity at the horizon |
| Delayed retry at end | Jobs whose retry timer has not elapsed at the horizon |
| Active at end | Jobs currently executing at the horizon |
| Peak backlog | Largest waiting-queue size observed after dispatch |
| Throughput | Completed jobs / scenario duration in seconds |
| Average queue wait | Mean accumulated waiting time across attempts for completed jobs |
| Average end-to-end latency | Mean final completion time minus first-arrival time for completed jobs |
| p95 / p99 latency | Nearest-rank percentiles of completed-job end-to-end latency |
| Worker utilization | Busy worker-slot microseconds / provisioned worker-slot microseconds, capped at 1 |

Queue-level metrics are included for each queue using the same definitions. Scenario-wide averages and percentiles are calculated over completed jobs across queues.

## Percentile calculation

For a sorted list with n completed latency values, nearest-rank percentile p is the value at one-based index:

```text
index = max(1, ceil(p × n))
```

The API converts this to a zero-based array index. If no jobs complete, average and percentile latency metrics are zero.

## Important interpretation notes

1. Completion percentiles exclude jobs still waiting, active, or delayed for retry at the horizon. Always read the end-of-run counters alongside latency.
2. Throughput divides by the configured horizon, not by the time of the last completion.
3. Utilization uses provisioned capacity as the denominator. A scheduled outage reduces new-dispatch capacity, but in-flight work continues to be counted as busy.
4. Retries are attempts of an existing job, not newly generated jobs.
5. Trace truncation is independent of metrics.
6. Peak backlog is measured after dispatch; an arrival that immediately starts does not produce a waiting backlog.
7. A job that is rejected before acceptance does not contribute to completion latency.
8. A retry that cannot re-enter a full queue is classified as dead-lettered, not as a new arrival rejection.
