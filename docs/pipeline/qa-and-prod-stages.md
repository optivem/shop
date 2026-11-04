# QA and Production Stages

Each flavor has one QA and one production workflow, both manual (`workflow_dispatch`) and both
deploying to Cloud Run:

| Workflow | Calls | What it does |
|---|---|---|
| `<flavor>-qa-stage.yml` | `_qa-stage.yml` → `_deploy-stage.yml` | Deploys an RC (the latest, or the `version` input) to QA, runs the smoke tests, and records the GitHub deployment to the `<flavor>-qa` environment |
| `<flavor>-qa-signoff.yml` | — | Approves or rejects the RC deployed to QA (`qa/signoff` commit status) |
| `<flavor>-prod-stage.yml` | `_prod-stage.yml` → `_deploy-stage.yml`, then `<flavor>-bump-patch-version.yml` | Deploys the latest QA-approved RC to production, runs the smoke tests, publishes the release (`<flavor>-v<x.y.z>` tag, image tags, component tags, GitHub release) and bumps the patch versions |

The order is: Docker Compose gates (automatic) → cloud acceptance stage (manual, creates the RC;
see [Acceptance Stage](acceptance-stage.md)) → QA stage → QA sign-off → production stage.
`meta-release-stage.yml` dispatches the six production stages for a full release.

QA and production each have their own Cloud Run services and Neon database per flavor
(`<system>-<flavor>-qa[-<role>]`, `<system>-<flavor>-production[-<role>]`) and share the one
Keycloak through their own realms (`shop-qa`, `shop-prod`). The smoke tests run anonymously:
the test harnesses only get tokens from the test realm. Production has no test users and no test
clients; in a monolith flavor that also means nobody can log in to production yet, because the
`shop-prod` realm has only the SPA client.

## Switching QA or production to Docker Compose

`_qa-stage.yml` and `_prod-stage.yml` each carry one clearly marked block,
`# === Docker Compose deploy (inactive) ===` … `# === End of Docker Compose deploy ===`. To deploy
with Docker Compose instead of Cloud Run, delete the active `deploy` job above it and uncomment the
block. It needs `DOCKERHUB_USERNAME` (variable) and `DOCKERHUB_TOKEN` (secret) instead of the GCP
settings.

**Docker Compose QA and production are only a zero-cost stand-in.** The stack runs on the GitHub
runner and is thrown away when the job ends: nothing persists between runs, and there is nothing
for a tester or a user to log in to. The release steps still run, so the pipeline shape stays the
same, but nothing is really deployed anywhere. That is different from the Docker Compose
acceptance gate, where a throw-away environment is exactly what's wanted: a real environment for
the length of the test run.
