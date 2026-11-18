# Runbook: a dependency is down

What the shop depends on in production, what the user sees when it fails, and what to do.

| Dependency | What the shop does when it fails | What you do |
|---|---|---|
| **ERP / Tax / Clock** (external systems; `-sim` service in the cloud stages) | Every call has a 2 s timeout; GETs retry 3 times with backoff and jitter; a circuit breaker per system opens after repeated failures. Place order fails fast with `503`, `Retry-After` and a problem+json message, never a guessed price ([tour](../tour.md), Resilience). The clock is the local system clock in production, so a Clock outage changes nothing. | Check the dependency's health URL (`<sim url>/erp/health`). Wait for it. In the cloud stages ERP and Tax are the `-sim` service, which the next deploy of the stage replaces. The breakers close on their own once calls succeed again; no app restart is needed. |
| **Neon Postgres** | `/readyz` turns non-200 (readiness checks the database; `/livez` stays 200), and requests that touch the database return `500` problem+json with a `traceId`. The Cloud Run services have no readiness probe configured, so traffic still reaches them. | Check [Neon status](https://neonstatus.com) and the project in the console. A free-plan compute suspends when idle and wakes on the next connection (a few seconds); a hard outage is Neon's to fix. If data is wrong rather than unreachable: [DB restore](db-restore.md). |
| **Keycloak** (shared Cloud Run service, `shop-prod` realm) | Nobody can sign in; multitier backends reject tokens they cannot verify (JWKS fetch fails) with `401`. Anonymous pages (product browse, `/livez`) keep working. | `gcloud run services describe <system>-keycloak` and its logs. Keycloak's own database is in the same Neon project, so check Neon first. Redeploying Keycloak means a prod or QA stage run (`deploy-keycloak`). |
| **GHCR** (image pulls) | Running revisions keep running; only a new deploy or a scale-out cold start that needs to pull fails. | Wait for [GitHub status](https://www.githubstatus.com). Don't dispatch deploys meanwhile. |
| **Secret Manager** | Only a deploy or a new revision reads it; running instances are unaffected. A deploy fails and the prod stage rolls back. | Wait; re-dispatch the deploy afterwards. |

## Triage order

1. `/livez` and `/readyz` on the production URL: `/livez` failing means the app itself is down (look at Cloud Run logs); `/readyz` failing with `/livez` fine means a dependency.
2. Cloud Run logs for the service, filtered on `severity>=ERROR`. Every log line is JSON with `trace_id`, and every error response carries the same id as `traceId`, so a user's error report leads to its log lines.
3. The table above for the dependency in question.

Locally, the same failures can be reproduced with the stub stacks (stop the `erp` stub container and place an order) and watched in Grafana with the `observability` profile ([observability](../observability.md), [SLOs and alerts](slos.md)).
