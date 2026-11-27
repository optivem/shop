# 2026-11-27 18:26:56 UTC — Quick deploy for UI changes

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-27T18:31:51Z`

## TL;DR

**Why:** Seeing a small UI change (e.g. the loading-spinner cold-start hint, `ade16113`) live currently means going through commit → acceptance → QA → prod, which takes over an hour. On the GitHub free plan (20 concurrent jobs) each job also queues 5–19 min behind the other pipelines.
**End result:** A manually dispatched workflow takes the current `main` commit of the reference variant (multitier-typescript: `system/multitier/backend-typescript` + `system/multitier/frontend-react`) and deploys it straight to real prod (`https://shop.optivem.com`) within minutes, with no tests at all, without disturbing regular releases.

## Outcomes

- One `workflow_dispatch` workflow (working name `quick-deploy-multitier-typescript.yml`) that you run from the Actions tab or with `gh workflow run`.
- It only builds the images and deploys them; a change is live in ~5–10 min, not >1h.
- No test stage of any kind runs: no commit-stage tests, no acceptance, QA or smoke suites. A deliberate untested path, for quick UI checks.
- It never cancels or corrupts a meta-prerelease / meta-release inner run, and the next regular release still produces the correct versioned image.
- Its target environment and its relation to the official release are documented in `docs/environments.md`.

## ▶ Next executable step (resume here)

Step 5: commit + push the workflow and docs, then `gh workflow run quick-deploy-multitier-typescript.yml -f ref=main`; watch it, then check https://shop.optivem.com shows the cold-start hint from ade16113.

## Steps

- [ ] Step 5: Dispatch it once with the loading-spinner change (`ade16113`) and check `https://shop.optivem.com` shows it.

## Decisions (resolved 2026-11-27, before execution)

- **DB migrations:** run them before the backend swap: point the existing `<system>-multitier-typescript-production-migrate` Cloud Run job at the freshly built db-migrate image and execute it (`--wait`). Keycloak and the simulators stay untouched.
- **Versioning:** quick images are tagged `quick-<sha>`; never touch `VERSION`, release/RC git tags, RC images or GitHub releases. The next regular prod release replaces the quick deploy.
- **Concurrency guard:** besides sharing the `multitier-typescript-prod-stage` group (`cancel-in-progress: false`), a pre-check job *outside* the group fails fast ("a release is in progress, try later") when any `multitier-typescript-prod-stage.yml` run is queued/in progress, so a waiting quick deploy can never replace a waiting prod-stage run.
- **Deploy mechanism (executor's choice):** `gcloud run services update <service> --image <quick image>` so the live service keeps its env vars/flags/secrets and nothing is duplicated from `_deploy-stage.yml`.
- **Step 5:** the agent dispatches it itself and verifies the site.
