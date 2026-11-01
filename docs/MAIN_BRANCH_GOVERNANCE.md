# Main-branch merge governance

## Code owner

`.github/CODEOWNERS` assigns `@nishanthn99` as the code owner for all files. GitHub can require approval from this code owner before a pull request is eligible to merge.

## Required repository settings

The repository's active `queueflux-main-protection` ruleset currently protects `main` from deletion and force pushes, but its required-status-checks list is empty and it does not currently require code-owner approval. CODEOWNERS alone does not enforce review or limit who can click Merge.

To enforce the intended policy in GitHub settings:

1. Open **Settings → Rules → Rulesets** and edit `queueflux-main-protection`.
2. Add a **Require a pull request before merging** rule (if available in the ruleset UI) and set **Required approvals** to at least `1`.
3. Enable **Require review from Code Owners**.
4. Ensure direct updates to `main` are blocked for non-authorized actors and no broad bypass actor can skip the rule.
5. Save the ruleset and verify a test PR cannot merge until `@nishanthn99` approves it.

## Important limitation: who may merge

GitHub's CODEOWNERS feature controls who must review changes; it does not by itself restrict the merge button to code owners. Required code-owner approval ensures a code-owner approval is needed before merge, but another user with write access may still merge after that approval. To make literally only code owners able to perform merges, use repository access controls so only the code-owner account/team has write/maintain/admin access, or use a supported organization/repository policy that restricts merge permissions. Avoid granting bypass to general write-role actors.

The connected GitHub integration can commit repository files and open pull requests, but it does not expose a write operation for editing repository rulesets or collaborator permissions. Those settings must be applied by a repository administrator in GitHub's UI.
