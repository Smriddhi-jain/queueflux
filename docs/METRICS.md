# Metrics

All timestamps are virtual microseconds.

- **Generated jobs:** arrival events processed within the simulation horizon.
- **Accepted jobs:** jobs admitted to the waiting queue (some start at the same virtual timestamp).
- **Completed jobs:** jobs whose completion event occurred by the simulation horizon.
- **Rejected jobs:** arrivals rejected because waiting depth was full.
- **Average queue wait:** mean of `startedAt - arrivedAt` for completed jobs.
- **End-to-end latency:** `completedAt - arrivedAt` for completed jobs.
- **p95/p99 latency:** nearest-rank percentile over completed-job latencies.
- **Peak backlog:** maximum waiting-queue size observed after an accepted arrival.
- **Throughput:** completed jobs divided by the configured simulation duration in seconds.
- **Worker utilization:** busy worker-slot microseconds divided by total configured slot microseconds; capped at 1.0.

The first release reports completed-job latency only. Jobs still active or waiting at the horizon are reported separately and do not contribute to completion percentiles. Warm-up periods and confidence intervals are planned features.
