# Runbook: deploy to production

Production is Cloud Run + Neon + Secret Manager ([Environments](../environments.md)); the pipeline shape is in [QA and Production Stages](../pipeline/qa-and-prod-stages.md). This page is the operator's checklist.

## Before you dispatch

1. The release candidate passed QA: `<flavor>-qa-stage.yml` is green and `<flavor>-qa-signoff.yml` set `qa/signoff=success` on it. The prod stage refuses anything else.
2. The pipeline is green: `gh run list -L 20`, plus the latest `meta-prerelease-stage.yml` run.
3. Any migration in the RC is rollback-safe (expand-contract, [CONTRIBUTING.md](../../CONTRIBUTING.md)). The [rollback](rollback.md) only moves app traffic; it never reverts the schema.
4. The monolith client secret in Secret Manager (`<system>-monolith-client-secret`) is not the test secret `shop-monolith-test-secret`. Under the prod profile the monolith refuses to start with it, so a deploy with it fails and rolls back. Compare without printing the value:

   ```bash
   gcloud secrets versions access latest --secret="<system>-monolith-client-secret" | cmp -s - <(printf %s shop-monolith-test-secret) && echo "TEST SECRET: rotate first" || echo "ok"
   ```

## Dispatch

- One flavor: `gh workflow run <flavor>-prod-stage.yml` (empty `version` = latest QA-approved RC, or `-f version=<flavor>-v1.2.3-rc.4`).
- All six: `meta-release-stage.yml`. Never dispatch a single flavor's prod stage while a meta release runs; they share the concurrency group, and the meta run's inner run gets cancelled.

## What the run does

1. `check` resolves the RC and refuses a version that is already released.
2. `snapshot` records the Cloud Run revisions that serve production now.
3. `deploy` (`_deploy-stage.yml`): Keycloak (the `shop-prod` realm reconcile, including brute-force settings), the simulators, the Flyway migrations, the services, then the smoke tests.
4. Green: `release` tags the images and git, signs the images with cosign, and creates the GitHub release; the patch version is bumped. Red: `rollback` puts the traffic back on the recorded revisions and nothing is released.

## After the run

- Green: open the production URLs from the run summary and check `/livez` and `/readyz` answer 200.
- Red: read the failing job. If `rollback` ran, production serves the previous release and you can diagnose at leisure (the failed revisions stay, without traffic). If `rollback` itself failed, follow the manual steps in [rollback](rollback.md) now.
