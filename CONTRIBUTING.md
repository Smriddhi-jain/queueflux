# Contributing to QueueFlux

Thanks for considering a contribution. QueueFlux is early-stage; focused changes and clear tests are especially valuable.

## Development setup

- Java 21+
- Maven 3.9+
- Node.js 22
- npm 10

```bash
mvn -B -ntp clean verify
cd web && npm install && npm run test:coverage && npm run lint && npm run build
```

## Architecture rules

- Keep `queue-model` immutable and free of Spring dependencies.
- Keep `simulation-engine` deterministic and free of wall-clock sleeps, network I/O, and persistence.
- Put vendor-specific behaviour in `simulation-profiles`, not in generic engine branches.
- Preserve event ordering and document any semantic change in `docs/SIMULATION_MODEL.md`.
- Keep scenario JSON versioned. Breaking schema changes require a schema-version change and migration notes.

## Branches and pull requests

Do not push changes directly to `main`. Create a short-lived branch from the latest `main`, for example:

```bash
git switch main
git pull --ff-only
git switch -c feature/short-description
```

Push that branch and open a pull request targeting `main`. Use the repository PR template and wait for the required CI and security checks before merging. See [docs/BRANCH_PROTECTION.md](docs/BRANCH_PROTECTION.md) for branch rules, and [docs/QUALITY_AND_SECURITY.md](docs/QUALITY_AND_SECURITY.md) for coverage and static analysis.

Before opening the PR:

1. Explain the problem and expected behaviour.
2. Add focused tests for correctness and determinism.
3. Run `mvn -B -ntp clean verify`.
4. Run `cd web && npm install && npm run test:coverage && npm run lint && npm run build`.
5. Review CodeQL/Semgrep and SonarQube results when available.
6. Update docs and example scenarios where relevant.
7. Do not include real credentials, customer payloads, or generated build artifacts.

## Good first contributions

- Implement priority-queue dispatch with deterministic tie-breaking.
- Add a log-normal service-time distribution.
- Add a global token-bucket rate limiter.
- Add retry/backoff semantics with seeded jitter.
- Add a metrics panel or scenario preset.
