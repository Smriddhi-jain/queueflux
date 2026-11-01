# BullMQ-inspired Profile: Scope and Differences

QueueFlux includes a named bullmq-inspired profile boundary. The profile currently validates the same scenario model as the generic worker-queue profile. It is a starting point for future comparative experiments, not an exact BullMQ emulator or a claim of API compatibility.

## What can be explored in QueueFlux today

- Queue-level worker capacity and concurrency.
- FIFO or workload-priority dispatch.
- A global queue token-bucket start-rate abstraction.
- Per-attempt failure probability.
- Retry attempt limits, exponential backoff and deterministic jitter.
- Worker-pool capacity reductions over planned time intervals.
- Backlog, throughput, waiting time and completed-job latency.

These are generic queueing abstractions useful for comparing policy choices. A BullMQ release can use different implementation details and state semantics.

## Important differences from a real BullMQ deployment

QueueFlux does not currently implement:
- Redis data structures, Lua scripts, distributed locks, lock renewal, or lock expiry;
- real worker processes and concurrency scheduling;
- BullMQ's exact waiting, active, delayed, prioritized, completed and failed state transitions;
- heartbeat/stalled detection and lock-token recovery;
- exact BullMQ rate-limiter window semantics or its global key behaviour;
- pause/resume, groups, parent/child flows, repeatable jobs, deduplication, metrics streams, or retention/removal policies;
- BullMQ version-specific defaults and error classification.

In BullMQ terminology, stalled is an event/condition associated with a job losing its lock; QueueFlux's planned worker outage is not the same thing. A worker outage in this simulator reduces new-dispatch capacity while active jobs finish. It must not be interpreted as BullMQ stalled-job recovery.

## Conformance work required before claiming compatibility

1. Pin a BullMQ package version and Redis version.
2. Build a matrix of behaviour tests for rate limiting, retries, backoff, priority order, stalled locks, and recovery.
3. Run the same seeded workload against a real BullMQ test deployment and QueueFlux.
4. Document each semantic divergence and compare distributions, not only single-run values.
5. Rename or version profile capabilities when the target broker changes.

Until that work is complete, use the label BullMQ-inspired and avoid using QueueFlux outputs as production predictions.
