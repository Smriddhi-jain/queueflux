# Architecture

QueueFlux is a modular monolith with a pure Java simulation engine.

```text
React UI -> Spring Boot API -> SimulationEngine -> EventScheduler / VirtualClock
                                  |                  -> workload streams
                                  |                  -> worker/queue runtime
                                  |                  -> metrics and bounded trace
                                  -> future persistence and experiment coordinator
```

## Dependency boundaries

- `queue-model` owns scenario vocabulary and validation.
- `simulation-engine` depends only on `queue-model` and the JDK.
- `simulation-profiles` maps generic or vendor-inspired configuration into engine concepts.
- `server` composes the modules and owns HTTP concerns.
- `web` communicates with the server over versioned JSON APIs.

Do not move provider/queue-specific policy branches into the generic simulation core. Add a profile or policy strategy instead.

## Execution isolation

The current endpoint executes synchronously. Before accepting public multi-user workloads, add bounded asynchronous execution, per-run cancellation, queue quotas, artifact retention, and persistence. Never expose arbitrary code execution as a scenario feature.
