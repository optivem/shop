# Runbook: roll back production

Rolling back means putting the Cloud Run traffic back on the revisions that served before. It never touches the database: migrations are expand-contract, so the previous release runs on the new schema ([CONTRIBUTING.md](../../CONTRIBUTING.md), rollback-safety table). Keycloak is shared by every flavor and is not rolled back either.

## Automatic: a failed deploy

`_prod-stage.yml` does this on its own. The `snapshot` job records, before the deploy, the serving revisions of every service the stack deploys (`<system>-<flavor>-production`, or its `-backend` and `-frontend`, plus `-sim`). When `deploy` fails or is cancelled (a service does not come up, or the smoke tests fail), the `rollback` job runs `gcloud run services update-traffic <service> --to-revisions <revision>=100` for each and lists them in the run summary. Nothing is released and the run stays red. On the very first production deploy there is nothing to roll back to, and the job is skipped.

## Manual: a release that went out and is bad

1. Find the previous good revision of each service:

   ```bash
   gcloud run revisions list --service <system>-<flavor>-production --region <region> --project <project>
   ```

   Revision names carry no version; `gcloud run revisions describe <revision> --format='value(spec.containers[0].image)'` shows the image tag (`<flavor>-v<x.y.z>-rc.<n>`).
2. Move the traffic, one service at a time; for multitier, the backend before the frontend:

   ```bash
   gcloud run services update-traffic <service> --to-revisions <revision>=100 --region <region> --project <project>
   ```

3. Check `/livez`, `/readyz` and the UI on the production URL.
4. The service now pins traffic to a named revision. The next prod stage deploy moves it to the new revision again (`gcloud run deploy` sends traffic to the latest). To return to "latest" without a deploy: `gcloud run services update-traffic <service> --to-latest`.
5. Fix forward: a new RC through QA. Do not re-dispatch the bad release; the prod stage refuses a version that is already released.

## Rollback drill (the documented test)

Run it on QA, never on production. It costs nothing beyond the QA deploys you do anyway.

1. Deploy two RCs to QA one after the other (`<flavor>-qa-stage.yml`), so the service has two revisions.
2. Roll the QA service back to the first revision with the manual step 2 above (service `<system>-<flavor>-qa[-<role>]`).
3. Run the smoke suite against the rolled-back QA service from your machine: point `systems.yaml` at the QA URLs the way the "Point systems.yaml at the Deployed Stack" step of `_deploy-stage.yml` does, then `gh optivem system-test run --suite smoke-real`. (Re-dispatching the QA stage is not a test of the rollback: it deploys again.)
4. Put QA back on the latest revision: `gcloud run services update-traffic <service> --to-latest`.
5. Write the date and the result in the run log or the plan you are working from.

The automatic path is proven by the first prod stage run that fails after a release exists; until then the `snapshot` output in every run shows what it would roll back to.
