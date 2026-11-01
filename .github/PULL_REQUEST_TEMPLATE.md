## Summary

<!-- Describe the problem, the change, and expected user-visible behaviour. -->

## Change type

- [ ] Bug fix
- [ ] Feature
- [ ] Documentation
- [ ] Build / CI / security
- [ ] Refactoring

## Validation

- [ ] Backend: `mvn -B -ntp clean verify`
- [ ] Frontend: `npm install && npm run test:coverage && npm run lint && npm run build`
- [ ] JaCoCo and Vitest coverage reports reviewed (when relevant).
- [ ] CodeQL and Semgrep findings reviewed (when relevant).
- [ ] SonarQube Cloud quality gate reviewed (when enabled).
- [ ] Tests added or updated for changed behaviour.
- [ ] Documentation and example scenarios updated where relevant.

## Simulation semantics

- [ ] No change to event ordering or deterministic RNG semantics.
- [ ] If changed, documented in `docs/SIMULATION_MODEL.md`.

## Security and repository hygiene

- [ ] No secrets, tokens, personal data, or real customer payloads are included.
- [ ] No generated build output or local environment files are included.
- [ ] New dependencies are justified and kept to the minimum required.
