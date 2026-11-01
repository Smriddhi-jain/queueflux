# Scenario Format v1

A scenario is a versioned JSON document containing `schemaVersion`, `name`, `durationMicros`, `seed`, `queues`, `workerPools`, and `workloads`.

- IDs are unique within their type.
- Each worker pool references a known queue.
- Each workload references a known queue and must end within the simulation horizon.
- The current engine requires one queue and exactly one worker pool for that queue.
- `CONSTANT` and `POISSON` arrivals both use `ratePerSecond`; the first emits at the workload start and then at fixed intervals, while the second samples exponential inter-arrival gaps.
- `CONSTANT` service uses `constantMicros`; `EXPONENTIAL` service uses `meanMicros`.

See `examples/constant-load/scenario.json` for a complete document.
