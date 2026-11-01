# Simulation Model

## Discrete-event time

The engine uses integer microseconds. It advances a virtual clock to the next scheduled event; it never sleeps in real time.

Events are ordered by `(timeMicros, phase, sequence)`. In the initial contract, completion events have phase 0 and arrivals phase 1, so a completion at time T can release capacity before an arrival at T is dispatched. Constant-rate arrivals begin at the workload start timestamp and use a fixed interval; Poisson arrivals sample exponential inter-arrival gaps. The workload end timestamp is exclusive.

## Job lifecycle

The first slice models `WAITING -> ACTIVE -> COMPLETED`, with `WAITING -> REJECTED` when waiting depth is full. Retry, delayed, stalled, cancelled, and dead-letter transitions are reserved for subsequent milestones.

## Queue capacity

`maxDepth` bounds waiting jobs, not active jobs. A queue with no waiting slots may still accept an arrival if a worker slot can start it immediately because dispatch occurs during arrival processing. Peak backlog is measured after immediate worker dispatch, so an arrival that starts without waiting does not count as backlog.

## Determinism

Arrival random streams are keyed by `arrival:<workloadId>`. Service-time streams are keyed by `service:<jobId>`. Each stream uses a documented SplitMix64 step and FNV-1a channel hashing rather than relying on a runtime-global random generator. This isolates unrelated random draws so a changed worker count does not change the arrival sequence or per-job service-time sample.

The simulator's deterministic contract applies to the same engine version and scenario schema. Changes to event phases, random-stream derivation, or distribution algorithms require release notes and golden tests.

## Current limitations

- Exactly one queue and one worker pool are supported.
- Priority discipline is accepted in the schema, but dispatch currently follows FIFO; priority dispatch is not yet implemented.
- Service-time and arrival distributions are currently constant/exponential and constant/Poisson respectively.
- Runs are synchronous and in-memory.
- The trace is capped and is not a complete durable event log.
