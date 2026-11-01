# QueueFlux

**Deterministic what-if experiments for asynchronous workloads.**

QueueFlux is an open-source workload experimentation platform. Define queues, workloads, worker pools, and processing-time distributions, then run a virtual-time simulation to inspect throughput, waiting time, tail latency, backlog, and worker utilization.

> **Status:** Early development (`0.1.0-SNAPSHOT`). The current engine supports one FIFO queue, one worker pool, constant or Poisson arrivals, constant or exponential service times, deterministic seeds, and a bounded event trace. Retry, rate-limit, multi-queue, and BullMQ-specific execution semantics are planned, not yet implemented.

## Why QueueFlux?

Queue dashboards show what happened in a real queue. Load generators exercise a real broker. QueueFlux explores hypothetical configurations using a deterministic, discrete-event model, so developers can compare worker capacity and workload assumptions without deploying a broker first.

QueueFlux is not a production benchmark and does not claim to predict a real broker exactly. Its BullMQ-inspired profile is a future behavior model, not a compatibility guarantee.

## Architecture

- `queue-model`: immutable scenario and workload contracts; no framework dependencies.
- `simulation-engine`: virtual clock, event priority queue, named seeded random streams, worker dispatch, and metrics.
- `simulation-profiles`: generic and BullMQ-inspired profile boundaries.
- `server`: Spring Boot REST API and engine wiring.
- `web`: React experiment workspace.

The engine does not sleep, create real worker threads for simulated jobs, or access external services. Virtual time moves from event to event.

## Quick start

Requirements: Java 21+, Maven 3.9+, Node.js 22+, npm 10+.

### Backend

```bash
mvn -pl backend/server -am package
java -jar backend/server/target/server-0.1.0-SNAPSHOT.jar
```

API health: `GET http://localhost:8080/actuator/health`

Run a simulation: `POST http://localhost:8080/api/v1/simulations/run` with a scenario JSON body. An example is in `examples/constant-load/scenario.json`.

### Frontend

```bash
cd web
npm install
npm run dev
```

Open `http://localhost:5173`. The frontend expects the API at `http://localhost:8080` by default. Set `VITE_API_URL` when the API is hosted elsewhere.

### Docker Compose

```bash
docker compose up --build
```

Open `http://localhost:3000`; API is available at `http://localhost:8080`.

## Example scenario

See [`examples/constant-load/scenario.json`](examples/constant-load/scenario.json). The example uses a 60-second Poisson arrival process with five workers and exponentially distributed service times.

## Simulation semantics (v0.1)

1. Events are ordered by virtual timestamp, event phase, then insertion sequence.
2. Completion events are processed before arrivals at the same timestamp.
3. A queue's `maxDepth` limits waiting jobs; active jobs do not consume waiting capacity.
4. Service-time randomness is derived from a named stream per job, so changing unrelated configuration does not alter a job's sampled service time.
5. Arrival randomness uses a named stream per workload.
6. The engine currently supports one queue and one worker pool; multi-queue orchestration is intentionally rejected rather than silently approximated.
7. Runs stop at the simulation horizon. Jobs still waiting or active at that point are reported separately.
8. Event traces are capped by `maxTraceEvents`; the cap affects presentation only, not simulation outcomes.

See [`docs/SIMULATION_MODEL.md`](docs/SIMULATION_MODEL.md) and [`docs/METRICS.md`](docs/METRICS.md).

## Current API

`POST /api/v1/simulations/run`

The request body is a `ScenarioDefinition`. The API currently executes synchronously and uses default resource limits. Persistence (including PostgreSQL), asynchronous execution, cancellation, and SSE progress are intentionally deferred until the experiment coordinator and immutable scenario-version model are implemented.

## Roadmap

- [x] Multi-module Java foundation
- [x] Deterministic virtual-time engine for one queue and one worker pool
- [x] Constant/Poisson arrivals; constant/exponential service times
- [x] Basic throughput, queue wait, latency percentiles, backlog, utilization
- [x] Spring Boot run endpoint
- [x] React experiment starter
- [ ] Priority dispatch and queue rate limiting
- [ ] Retry/backoff and dead-letter policy simulation
- [ ] Worker outages and stalled-job recovery
- [ ] Multi-queue and multi-tenant models
- [ ] PostgreSQL scenario/run persistence and asynchronous experiments
- [ ] Statistical replications and paired configuration comparison
- [ ] BullMQ-inspired policy mapping with conformance tests
- [ ] CLI and CI scenario runner

## Contributing

See [`CONTRIBUTING.md`](CONTRIBUTING.md). Please open an issue before large changes to the event model or scenario schema.

## License

Apache-2.0. See [`LICENSE`](LICENSE).
