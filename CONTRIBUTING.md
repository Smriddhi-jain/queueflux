# Contributing to QueueFlux

Thanks for considering a contribution. QueueFlux is early-stage; focused changes and clear tests are especially valuable.

## Development setup

- Java 21+
- Maven 3.9+
- Node.js 22
- npm 10

```bash
mvn -pl backend/server -am test
cd web && npm install && npm run build
```

## Architecture rules

- Keep `queue-model` immutable and free of Spring dependencies.
- Keep `simulation-engine` deterministic and free of wall-clock sleeps, network I/O, and persistence.
- Put vendor-specific behaviour in `simulation-profiles`, not in generic engine branches.
- Preserve event ordering and document any semantic change in `docs/SIMULATION_MODEL.md`.
- Keep scenario JSON versioned. Breaking schema changes require a schema-version change and migration notes.

## Pull requests

1. Explain the problem and expected behaviour.
2. Add focused tests for correctness and determinism.
3. Run backend tests and frontend production build.
4. Update docs and example scenarios where relevant.
5. Do not include real credentials, customer payloads, or generated build artifacts.

## Good first contributions

- Implement priority-queue dispatch with deterministic tie-breaking.
- Add a log-normal service-time distribution.
- Add a global token-bucket rate limiter.
- Add retry/backoff semantics with seeded jitter.
- Add a metrics panel or scenario preset.
