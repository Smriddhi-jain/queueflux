# Build artifacts and test reports

QueueFlux uses GitHub Actions artifacts for CI outputs. This is a GitHub-native alternative to running a separate artifact server for compiled outputs and test reports.

## CI outputs

- `queueflux-backend-jars`: JAR files produced by Maven modules under `backend/*/target/` after a successful backend build.
- `queueflux-backend-test-reports`: Maven Surefire and Failsafe reports, when present. The upload should run even when tests fail so diagnostics are preserved.
- `queueflux-frontend-dist`: production assets from `web/dist/` after a successful frontend build.

Artifacts should be retained for 14 days. Open the repository's Actions tab, select a workflow run, and download artifacts from its Artifacts section. Pull-request builds can upload artifacts too, allowing reviewers to inspect outputs before merge.

## Cost and limitations

Actions artifacts are temporary CI outputs, not a permanent Maven or npm package registry. They are suitable for build products, test reports, and debugging. For long-lived versioned binaries, use GitHub Releases; for versioned dependency packages, use GitHub Packages.

Check current public-repository limits in [GitHub Actions billing documentation](https://docs.github.com/en/billing/concepts/product-billing/github-actions).

## Troubleshooting

- If test reports are missing, check the Maven logs; modules without tests may not create report files.
- If JARs are missing, inspect Maven packaging output.
- Artifacts expire after 14 days; download anything needed longer-term.
