# Architecture

QueueFlux is a modular monolith with a pure Java discrete-event simulation engine and a React web client.

§§§text
React UI -- REST/SSE --> Spring Boot API --> SimulationEngine
                              |                  |
                              |                  +-- EventScheduler
                              |                  +-- VirtualClock
                              |                  +-- QueueRuntime(s)
                              |                  +-- deterministic random streams
                              |                  +-- metrics / trace
                              |
                              +-- bounded run executor
                              +-- JDBC repository --> PostgreSQL JSONB
§§§

## Dependency rules

- queue-model owns scenario vocabulary and validation.
- simulation-engine depends only on queue-model and the JDK.
- simulation-profiles owns behaviour-profile boundaries and vendor-inspired mapping.
- server composes modules and owns HTTP, persistence, async orchestration and SSE.
- web consumes the versioned JSON API.

## Execution model

Every queue has exactly one worker pool in this release. Workloads may share a queue. Events from all queues are ordered by virtual time and phase in one scheduler, while queue state and worker capacity remain independent. The same run can therefore compare independent queue topologies under one deterministic scenario clock.

## Persistence boundary

PostgreSQL stores run lifecycle records, submitted scenario JSON and final result JSON. The engine is stateless between calls and does not store each queue event in the database. Trace output is bounded and is included in the result snapshot.

## Safe extension points

- Add queue policies as immutable fields in queue-model.
- Implement their state transitions in simulation-engine.
- Add profile constraints in simulation-profiles.
- Expose configuration/results via server and web.
- Add deterministic tests and document event phase/metric changes in the same change.
