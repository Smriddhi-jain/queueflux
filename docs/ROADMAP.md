# Roadmap and Implementation Status

This document distinguishes features that exist in the current branch from ideas that still need work.

## Implemented in the current development baseline

- [x] Multi-module Java 21 project with framework-independent model and engine.
- [x] Virtual-time discrete-event engine with deterministic timestamp/phase/sequence ordering.
- [x] Named seeded random streams for arrivals, service samples, failure injection and retry jitter.
- [x] Constant and Poisson arrival processes.
- [x] Constant and exponential service-time distributions.
- [x] Multiple independent queues, one worker pool per queue.
- [x] FIFO and workload-level priority dispatch.
- [x] Waiting-depth admission limit.
- [x] Per-queue global token-bucket job-start rate limiting.
- [x] Configurable failure probability, bounded retries, exponential backoff and deterministic jitter.
- [x] Dead-letter outcomes for exhausted attempts and retry re-admission overflow.
- [x] Planned worker outages reducing capacity for new dispatch.
- [x] Scenario and per-queue throughput, latency, backlog, utilization and failure metrics.
- [x] Bounded trace independent of simulation metrics.
- [x] Spring Boot synchronous execution endpoint.
- [x] PostgreSQL JSONB persistence for asynchronous run metadata, scenario and result.
- [x] Bounded async executor, run-status endpoint and SSE progress events.
- [x] Seeded statistical replications endpoint with means and standard deviations.
- [x] React scenario editor, advanced JSON mode, run progress and results.
- [x] Docker Compose, example scenario, Maven tests, frontend production CI and project documentation.

## Remaining engineering work

### Correctness and modelling
- [ ] Property-based testing across random scenario combinations.
- [ ] Golden compatibility suite against pinned BullMQ and Redis versions.
- [ ] Real-worker telemetry calibration and model-fit reporting.
- [ ] Worker-specific identity, active-job interruption, lock/heartbeat and recovery semantics.
- [ ] Warm-up periods, confidence intervals and paired scenario comparison using common random numbers.
- [ ] Multiple pools per queue, shared worker pools and cross-queue fairness.

### API and run orchestration
- [ ] Authentication, authorization, tenant isolation and per-user quotas.
- [x] Single-process restart recovery by deterministic replay from virtual time zero.
- [ ] Distributed recovery with leases, heartbeats and duplicate-run protection.
- [ ] Run cancellation, timeout, queue-depth controls and idempotency keys.
- [ ] Durable progress history and paginated trace/artifact storage.
- [ ] Database migrations with Flyway/Liquibase and retention/archival jobs.
- [ ] Distributed workers for horizontally scaled simulation execution.

### Product and developer experience
- [ ] Scenario builder for queues, priorities, retry policies and outages.
- [ ] Compare two configurations in one UI with charts and confidence intervals.
- [ ] CLI and scenario-as-code runner for local/CI use.
- [ ] Exportable reports (JSON/CSV) and saved experiment collections.
- [ ] Benchmark suites and documented example studies.
- [ ] Queue metrics ingestion adapters and calibration tooling.

## Explicitly not production-ready

The current API is suitable for local experimentation and academic development. It is not ready for untrusted, multi-tenant or internet-facing workloads until authentication, authorization, quotas, operational recovery, migration strategy, and resource admission controls are implemented.
