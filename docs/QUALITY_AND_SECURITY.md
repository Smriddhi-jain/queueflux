# Quality, coverage and security

QueueFlux uses a layered code-quality pipeline.

## Continuous validation

The `CI` workflow runs on pull requests targeting `main` and pushes to `main`.

| Area | Tool | Command / output |
|---|---|---|
| Backend tests | Maven Surefire / JUnit | `mvn -B -ntp clean verify` |
| Backend coverage | JaCoCo 0.8.16 | XML and HTML under each module's `target/site/jacoco/` |
| Frontend tests | Vitest 5 | `npm test` |
| Frontend coverage | Vitest V8 provider | `web/coverage/lcov.info` and HTML report |
| Frontend quality | ESLint + TypeScript + Vite | `npm run lint`, `npm run build` |

Coverage reports are uploaded as GitHub Actions artifacts for 14 days. Coverage is reported, but no arbitrary percentage threshold is enforced yet. The current test suite is a starting baseline: the Java suite is small, and most React UI/API behaviours still need dedicated tests. Expand tests before introducing a global minimum.

Local commands:

```bash
mvn -B -ntp clean verify
cd web
npm install
npm run test:coverage
npm run lint
npm run build
```

## SonarQube Cloud

The optional SonarQube Cloud workflow uses SonarScanner for Maven, imports JaCoCo XML, and imports frontend LCOV coverage. The root Maven project enables `sonar.maven.scanAll` so non-JVM files under the root project can be included in the scan.

To enable:

1. Sign in to [SonarQube Cloud](https://sonarcloud.io/) with GitHub.
2. Import the public `nishanthn99/queueflux` repository and create the QueueFlux project. Confirm that it qualifies for SonarQube Cloud's open-source offering.
3. Generate a project analysis token.
4. In GitHub **Settings → Secrets and variables → Actions**:
   - Add repository secret `SONAR_TOKEN`.
   - Add repository variable `SONAR_ENABLED` with value `true`.
   - Add repository variable `SONAR_ORGANIZATION` with the organization key.
   - Add repository variable `SONAR_PROJECT_KEY` with the exact project key shown by SonarQube Cloud.
5. Run the **SonarQube Cloud** workflow manually or open a PR and confirm the analysis and quality gate in SonarQube Cloud.

The job is intentionally skipped until `SONAR_ENABLED=true` and the token/variables are configured. Fork PRs are skipped because GitHub does not expose repository secrets to them. The scan waits for the quality-gate result and fails if the configured gate fails.

Never commit a token or place it in workflow YAML or repository variables.

## GitHub CodeQL

The **Code Security** workflow analyzes:

- `java-kotlin` using Java autobuild.
- `javascript-typescript` using interpreted-language extraction.
- The `security-extended` query suite.

It runs on same-repository PRs targeting `main`, pushes to `main`, weekly, and by manual dispatch. Findings are uploaded to **Security → Code scanning**. CodeQL upload on fork-originated PRs is skipped because the fork workflow token cannot write security events.

## Semgrep Community SAST

The same security workflow runs Semgrep Community Edition with its public `auto` ruleset. The CLI is pinned to 1.183.0 and SARIF results are uploaded to GitHub code scanning when the workflow has permission. Fork PRs are still scanned, but their SARIF upload is skipped.

Semgrep initially runs in reporting mode: a rule match does not fail the job. Review findings and false positives before deciding whether particular rules should block merging.

## Recommended required checks for main

After each workflow has completed at least once, configure the `main` ruleset to require:

- Backend tests and JaCoCo coverage
- Frontend tests, lint, build and coverage
- CodeQL analysis for Java/Kotlin
- CodeQL analysis for JavaScript/TypeScript
- Semgrep Community SAST

Add SonarQube Cloud analysis only after the Sonar project and repository secret/variables are configured and the workflow is enabled. Select exact check names from the PR Checks tab; do not require a skipped optional job.

## Security notes

The workflows use least-privilege permissions and pin third-party actions to full commit SHAs. SonarCloud is optional because it needs a user-created token. No credentials are stored in the repository.
