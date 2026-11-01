# Publishing QueueFlux to GitHub

1. Open https://github.com/new while signed in as the intended owner.
2. Set repository name to `queueflux`, visibility to **Public**, and do not initialize it with a README, license, or `.gitignore` (those files already exist here).
3. From this checkout, set the remote and push:

   ```bash
   git remote add origin https://github.com/<owner>/queueflux.git
   git push -u origin main
   ```

4. In repository Settings, enable Issues and Discussions if desired, add topics `discrete-event-simulation`, `queueing-theory`, `workload-testing`, `java`, `spring-boot`, and `react`, and review Actions permissions.
5. Check the first GitHub Actions run. The first version is intentionally early-development software; do not call it production-ready until persistence, asynchronous run orchestration, API safeguards, and the remaining simulation policies are implemented.
