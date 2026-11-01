# Protecting the main branch

QueueFlux changes should reach `main` through pull requests. The repository workflow files and this guide prepare the checks, but **a GitHub repository ruleset/branch protection rule must be enabled in repository settings** for GitHub to reject direct pushes. Configure that rule before relying on it as an enforcement boundary.

## Recommended ruleset

In GitHub, open **Settings → Rules → Rulesets → New ruleset → New branch ruleset**.

Configure:

| Setting | Value |
|---|---|
| Ruleset name | Protect main |
| Enforcement status | Active |
| Target branches | Include `main` |
| Require a pull request before merging | Enabled |
| Required approvals | 0 for a single-maintainer repository; use 1 or more when another maintainer is available to review |
| Require status checks to pass | Enabled |
| Require branches to be up to date | Enabled |
| Require conversation resolution | Enabled |
| Restrict deletions | Enabled |
| Block force pushes | Enabled |

For required status checks, select both jobs from **Continuous Testing** after that workflow has completed at least once:

- `Backend tests`
- `Frontend lint and production build`

The existing `CI` workflow also runs on main pull requests. The two checks above are the dedicated continuous-testing gates and include the full Maven verification, frontend lint/build, and high-severity npm advisory check.

Keep the ruleset applicable to administrators. Leave the bypass list empty unless there is a deliberate, documented emergency process. Do not add a general administrator bypass just to make routine changes easier.

## Why the approval count is configurable

GitHub does not count the pull-request author's own approval as an independent review. For a repository maintained by one person, requiring one approval can make normal merges impossible. Keep required approvals at zero until a second trusted maintainer is available; the required CI checks, PR-only rule, up-to-date branch rule, and conversation resolution still provide useful safeguards.

## Pull-request workflow

1. Update local `main` and create a short-lived feature branch.
2. Commit the change to that branch; do not push directly to `main`.
3. Open a PR targeting `main` and complete the PR template.
4. Wait for both Continuous Testing checks to pass.
5. Resolve review conversations and update the branch with the latest `main` when GitHub requests it.
6. Merge the PR using the repository's allowed merge strategy.

## Workflow security

The continuous-testing workflow uses read-only repository permissions, disables persisted checkout credentials, and pins actions to full commit SHAs. Dependabot checks the GitHub Actions, npm, and Maven manifests weekly and proposes dependency update PRs.

For details about workflow dependency pinning, see GitHub's [secure use reference](https://docs.github.com/en/actions/reference/security/secure-use).


## Additional quality and security checks

After the quality/security PR is merged and each workflow has run at least once, add the following exact job checks to the `main` ruleset:

- Backend tests and JaCoCo coverage
- Frontend tests, lint, build and coverage
- CodeQL analysis for Java/Kotlin
- CodeQL analysis for JavaScript/TypeScript
- Semgrep Community SAST

Add SonarQube Cloud analysis only after the project and repository secret/variables are configured and the workflow is enabled. See [QUALITY_AND_SECURITY.md](QUALITY_AND_SECURITY.md).
