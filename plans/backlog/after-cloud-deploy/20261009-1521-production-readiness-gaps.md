# 2026-10-09 15:21:17 UTC — Production-readiness gaps (shop)

## TL;DR

**Why:** A review of the Java monolith against a typical production Spring Boot / JS app found gaps in data correctness, resilience, observability, security hardening, supply chain, and ops. The review did not cover .NET/TS/multitier, so parity is unverified.
**End result:** Six independently shippable chunks are done, each in Java first and then verified or ported to .NET and TypeScript. The shop reads as a production-grade reference, and chunks 1–3 exist as ATDD scenarios.

## Outcomes

- Placing an order is atomic: the order and the coupon usage change commit together or not at all.
- Concurrent orders can never exceed a coupon's `usage_limit`, and concurrent order updates can't silently overwrite each other.
- Retrying `POST /api/orders` with the same idempotency key never creates a duplicate order.
- List endpoints are paginated and order search is index-friendly.
- A slow or failing ERP/tax/clock dependency degrades gracefully (shared pooled client, timeouts, retry/circuit breaker) instead of exhausting threads.
- Metrics, trace/correlation IDs and structured logs are available. Actuator is locked down, with liveness/readiness split, and a `prod` profile drops dev settings.
- The app refuses to start in prod with default or missing secrets, and has rate limiting, security headers, a CORS policy and a deliberate Swagger policy.
- Dependency, code, secret and image scanning run automatically, with pinned base images and dependencies, SBOM/signing, `SECURITY.md` and `CODEOWNERS`.
- A deployable prod target (Cloud Run / Cloud SQL / Secret Manager) exists, with a rollback and expand/contract migration policy, runbooks, backups, SLOs/alerts, and load/DAST checks.
- Each chunk's behaviour is the same across Java, .NET and TypeScript, or the differences are documented.

## Finding-by-language table (item 0.1, audited 2026-11-17)

Backends: MJ = `system/monolith/java`, BJ = `multitier/backend-java`, CJ = `multitier/backend-clean-java`, MD = `monolith/dotnet`, BD = `multitier/backend-dotnet`, MT = `monolith/typescript`, BT = `multitier/backend-typescript`. "gap" = the later item must touch it; "ok" = already in place.

| Finding (item) | MJ | BJ | CJ | MD | BD | MT | BT |
|---|---|---|---|---|---|---|---|
| Trace id in ProblemDetail body (3.2) | gap (`timestamp`/`errors` only) | gap | gap | gap (hand-written `WriteAsJsonAsync`, no `traceId`) | gap | gap (`lib/errors.ts`) | gap (`global-exception.filter.ts`) |
| Actuator / health (3.4) | `/health` public stub; actuator `health,info` behind auth, `show-details: always`; no liveness/readiness split | same | same | `/health` stub only (no health checks, no probes split) | same | `app/health` stub | `/health` stub |
| Dev settings with no prod profile (3.5) | gap (`show-sql`, `format_sql`, `thymeleaf.cache: false`) | gap (`show-sql`, `format_sql`) | gap (`show-sql`, `format_sql`) | ok (ASP.NET env model; `appsettings.Development.json` only in Development) | ok | ok (`NODE_ENV=production` in the image) | ok (TypeORM `logging` off when `NODE_ENV=production`) |
| Secret / config defaults (4.1) | gap (`AUTH_CLIENT_SECRET` test default, DB password `app`, issuer `localhost`) | gap (DB password `app`, issuer `localhost`; resource server, no client secret) | gap (same as BJ) | gap (DB password `app`; client secret has no default but is not checked) | gap (DB password `app`) | gap (client + session secret test defaults, DB password `app`) | gap (DB password default) |
| Security headers (4.3) | partial (Spring Security defaults: nosniff, `X-Frame-Options: DENY`, HSTS on https; no CSP / Referrer-Policy) | partial (same) | partial (same) | gap (none) | gap (none) | gap (none; Next.js sends `X-Powered-By`) | gap (none) |
| CORS policy (4.3) | ok (same-origin, no CORS = cross-origin denied) | ok (explicit `allowed.origins` list) | ok (`CorsConfig`) | ok (same-origin) | ok (`ALLOWED_ORIGINS` / `Cors:AllowedOrigins`) | ok (same-origin; CSRF guard in `middleware.ts`) | ok (`enableCors` origin list) |
| Swagger / OpenAPI exposure (4.4) | authenticated (any user), not admin-only | same | same | `RequireAuthorization()` (any user) | same | authenticated (not in the public auth policy) | authenticated (global guard) |
| Rate limiting (4.2) | none | none | none | none | none | none | none |
| Login surface for 4.2 | OIDC redirect only; credentials are entered at Keycloak | none (bearer only) | none (bearer only) | OIDC redirect only (`/sign-in`) | none (bearer only) | OIDC redirect (`/login`, `/login/callback`) | none (bearer only) |

**Consequence for 4.2:** no backend accepts a password, so there is no in-app "login attempt" to count: the multitier backends only validate bearer tokens and the monoliths only redirect to Keycloak. See 4.2 for the open question this raises.

## ▶ Next executable step (resume here)

All items are done (U9 runs 1–3, 2026-11-17/18). **One verification left (not executable now, never dispatch prod for it):** the next approved meta-release confirms on production what no local or CI run can: (1) every flavor's `snapshot` job lists the serving revisions, (2) `release` signs the images (`cosign verify`, command in `SECURITY.md`), (3) `deploy-keycloak` PUTs the brute-force settings on `shop-prod` (4.2). The `rollback` job only runs on a red deploy; its first real run is the first failed prod deploy after a release exists. Then delete this plan. **Checked 2026-11-18 (U9 run 3):** the prod `shop-monolith-client-secret` in Secret Manager is not `shop-monolith-test-secret` (compared without printing, 4.1); `dast.yml` run 44333808708 green (ZAP step + summary work; findings in followups #87); `--sample` green on all six latest stacks after the base-image digest pins (5.6).

## Steps

Each chunk is independently executable, and the chunks can be done in any order. Within a chunk, do Java first (including the multitier backends), then .NET, then TS. After each chunk run the pre-commit verification from CLAUDE.md (`./compile-all.sh`, then `--sample` system tests per affected language).

### Chunk 1 — Data correctness (ATDD scenarios)
Steps 1.1–1.6 are done (2026-11-03); the child plan `20261009-1545-production-readiness-chunk1-data-correctness.md` is deleted. The decisions (idempotency per user with a 24 h TTL, 422 on a reused key with a different body, optional 1-based `page`/`size` with default 50 and cap 100, `LIKE '%x%'` search accepted as is) live in the code and its tests. 1.7 (id-based `equals`/`hashCode` on the JPA entities, `8617106e`) and 1.8 (hourly idempotency-key purge in all six backends, `991b9eae`) are done (U9, 2026-11-17).

### Chunk 2 — Resilience
2.1 (shared pooled client, configurable timeouts) and 2.2 (GET-only retry + circuit breaker) were delivered by SHOW: `GatewayHttpClient` + `ResilienceSettings` (Resilience4j) in the three Java backends, `Api/Resilience/ResilienceExtensions.cs` in both .NET backends, `resilience.ts` in both TS backends, with `ErpGatewayResilience*` tests.
2.3 is done (U9 run 2, 2026-11-17): every backend already read the local system clock in `real` mode (`Instant.now()`, `DateTime.UtcNow`, `new Date()`), and production deploys `EXTERNAL_SYSTEM_MODE=real`, so the external clock was only ever used by `stub` stacks. Now enforced: under the prod profile all seven `ProductionConfigGuard`s refuse to start when `EXTERNAL_SYSTEM_MODE` is set to anything but `real`; recorded in `docs/environments.md` ("Clock").

### Chunk 3 — Observability and config profiles
3.1 (metrics), the trace-propagation and log parts of 3.2, and 3.3 (structured JSON logs) were delivered by SHOW: OTel/Micrometer pushed over OTLP (`OtlpExportEnvironmentPostProcessor`, `micrometer-registry-otlp`; metrics are not exposed over HTTP, Java actuator exposes only `health,info`), JSON logs with `trace_id`/`span_id` always on (`logging.structured.format.console: logstash`, `TraceJsonConsoleFormatter`, `logger.ts` / `json-logger.ts`), viewable through the opt-in `otel-lgtm` compose profile. Done in U9 (2026-11-17): 3.2 `traceId` in every ProblemDetail body (`615ae9a4`), 3.4 public `/livez` + `/readyz` in all seven backends with the Docker `HEALTHCHECK` on `/livez` and Java health details ADMIN-only (`707e37a6`), 3.5 `application-prod.yml` in the three Java backends (`SPRING_PROFILES_ACTIVE=prod`). **3.5 deviation, noted:** the profile is set by `_deploy-stage.yml` for the production stage only, not in the docker/compose pipeline configs as first written, because 4.1's fail-fast runs under the same profile and the pipeline stacks must keep their test defaults.

### Chunk 4 — Security hardening
4.1 is done (U9, 2026-11-17): under the prod profile (Java `SPRING_PROFILES_ACTIVE=prod`, .NET/TS `APP_PROFILE=prod`, both set by the production deploy only) every backend refuses to start on a missing/test-default DB password, a missing/localhost issuer, or (monoliths) the test-realm client secret. The TS monolith's `AUTH_SESSION_SECRET` is not checked yet: production never sets it (REL #76).
4.3 is done (U9 run 2, 2026-11-17): all seven backends send one **enforced** CSP (`default-src 'self'`, `'unsafe-inline'` for script/style because the monolith pages and Swagger UI carry inline script and style attributes, `img-src`/`font-src 'self' data:`, `object-src 'none'`, `base-uri 'self'`, `frame-ancestors 'none'`; `form-action` left out because browsers apply it to the logout redirect to Keycloak), `Referrer-Policy: strict-origin-when-cross-origin`, HSTS on every response (TLS ends at Cloud Run), `nosniff` and `X-Frame-Options: DENY`. Java via Spring Security `headers()` (`CJ` has no Spring Security, so a `SecurityHeadersFilter`; the 0.1 table's "partial" for CJ was wrong), .NET via `UseShopSecurityHeaders()` plus no `Server` header, TS monolith via `next.config.mjs` `headers()` plus `poweredByHeader: false` (`'unsafe-eval'` only under `next dev`), Nest via `useSecurityHeaders()` plus no `X-Powered-By`. Enforced, not report-only: every page loads only same-origin resources, and the TS build was checked with `next start` + curl. CORS was already explicit (0.1 table), unchanged. `frontend-react` (nginx) is not covered: REL #78.
4.2 is done (U9 run 2, 2026-11-17): Keycloak brute-force detection in `shop-realm.json` (10 failures, lockout 1 to 15 min, never permanent), so every realm built from it has it; `prod-realm-check.jq` fails the image build without it; `deploy-keycloak`'s prod reconcile step PUTs the same settings on the existing `shop-prod` realm (imports never update a realm; inert until a production deploy). Existing cloud `shop`/`shop-qa` realms keep running without it until they are re-created (test realms, accepted). The opt-in edge policy is `docs/operations/edge-rate-limiting.md` (Cloud Armor about $24/month and above the €1 kill-switch, or a $0 fronting proxy), linked from the README. **Shortcut, noted:** non-login endpoints stay unlimited by default. No system test fails a login on purpose, so the threshold cannot lock out `customer1`/`admin1`.
4.4 is done (U9 run 2, 2026-11-17): **disabled in prod** (not admin-only). Under the prod profile the Java backends set `springdoc.api-docs.enabled` / `swagger-ui.enabled: false` in `application-prod.yml`, .NET's `MapShopOpenApi` maps nothing (`ProductionConfigGuard.IsProdProfile`), the TS monolith's `/v3/api-docs` answers 404, and Nest skips `setUpOpenApi` and 404s the document. Local and pipeline stacks are unchanged: signed-in callers only (B-KC decision 1); no system test reads the docs. Note: `backend-clean-java` has no auth, so outside prod its docs stay public, as before.

### Chunk 5 — Supply chain and repo hygiene
5.1 (grouped Dependabot for gradle, nuget, npm, docker, github-actions: `.github/dependabot.yml`), CodeQL (`codeql.yml`), 5.3 (Trivy, CRITICAL/HIGH with a fix, report-only by design, in the standalone `supply-chain.yml`) and the SBOM part of 5.4 (Syft CycloneDX in `supply-chain.yml` and `release-sbom.yml`, BuildKit `sbom: true` + `provenance: mode=max` in the commit stages) were delivered by SHOW.
5.4 is done (U9 run 3, 2026-11-18, `a540d1d6`): the `release` job of `_prod-stage.yml` signs each released image digest with cosign keyless (GitHub OIDC, Sigstore, $0; `id-token: write` on the job); verify command in `SECURITY.md`. actionlint only; the real run is the next meta-release (▶ block).
5.5 is done (U9 run 2, 2026-11-17): `SECURITY.md` (private reporting via the Security tab, what is in place, the committed test credentials explained) and `.github/CODEOWNERS` (`*` to the maintainer). Private vulnerability reporting was turned on 2026-11-17 (user-approved), so the `SECURITY.md` link works.
5.6 is done (U9 run 3, 2026-11-18, `149cc519`, `8a94b4c1`): every Dockerfile `FROM` is `tag@sha256:<index digest>` (Dependabot's docker updater bumps the digest; noted in `dependabot.yml`), and the commit stages' pre-pull steps read the refs from the Dockerfile instead of repeating tags; all five npm projects pin exact versions (from their lockfiles, lockfile roots synced, no resolved version changed) with `save-exact=true` in a per-project `.npmrc`. Gates: compile-all green, `--sample` green on all six latest stacks.

### Chunk 6 — Deploy and operations
6.1 was delivered by the cloud deploy (`terraform/main.tf`: project, WIF, service accounts, Secret Manager; `terraform/neon.sh` for Neon; Cloud Run services and `--min-instances`/`--max-instances` set by `_deploy-stage.yml`). 6.2 is in `CONTRIBUTING.md` (rollback-safety table and expand-contract worked examples). The k6 half of 6.7 is `load-test.yml` (SHOW).
6.3 is done (U9 run 3, 2026-11-18, `a540d1d6`): `_prod-stage.yml` gets a `snapshot` job (serving revisions of every stack service before the deploy; fails loud on a gcloud error, skips services that don't exist yet) and a `rollback` job (on a failed or cancelled deploy, `update-traffic --to-revisions` back to them; nothing is released). Keycloak and the schema are not rolled back (shared; expand-contract). Manual rollback and the QA drill: `docs/operations/rollback.md`. actionlint only; confirmed by the next meta-release (▶ block).
6.4 is done (U9 run 3): `docs/operations/deploy.md`, `rollback.md`, `db-restore.md`, `dependency-outage.md`, linked from the README. **Note:** the deploy/DB-restore/dependency-outage runbooks and the 6.6 alert rules + compose mount were swept into another session's unrelated commit `3751adfd` ("Daily 2026-11-18: …") from the shared tree; the content is this unit's, history left as is.
6.5 is done (U9 run 3, $0): `docs/operations/db-restore.md` (Neon point-in-time restore via a check branch then branch restore, optional `pg_dump`, quarterly drill that only creates and deletes a branch). The drill itself has not been run (needs Neon auth; user's call).
6.6 is done (U9 run 3, $0, `f6451ba9` + `3751adfd`): `docs/operations/slos.md` (availability 99.5%, latency 95% within 512 ms) and Grafana alert rules in `docker/observability/grafana/alerting/shop-slo-alerts.yaml`, mounted into `otel-lgtm` in all 24 compose files (block stays identical). The rules use Tempo span metrics because the native HTTP metrics differ per language (Java ms without buckets, .NET seconds, Next.js none). Verified locally on the TS monolith: rules provisioned, fast burn went Firing with the ERP stub stopped.
6.7 is done (U9 run 2, 2026-11-17): `.github/workflows/dast.yml`, an opt-in `workflow_dispatch` ZAP baseline scan (passive rules only, 1-minute spider) of one monolith stub stack built on the runner, like `load-test.yml`; report-only (alert counts in the job summary, HTML/JSON/Markdown reports as an artifact), fails only when ZAP itself errors. Dispatched 2026-11-18: run 44333808708 green (0 FAIL, 11 WARN, 56 PASS; followups #87); `docs/tour.md` row added next to k6 (`98fdcbde`).

## Resolved decisions

Decided (user, 2026-11-16, meta-plan G0 answer 7, plus the meta-plan's U9 chunk order):

- **Scope:** shop only. Chunks 5–6 apply to the shop itself, not to the repos gh-optivem scaffolds; scaffolding is a separate later decision.
- **Order of chunks:** 0, 1, 3, 4, 2, 5, 6 (Chunk 0 fixes the scope; 1 is the real correctness risk; 3 and 4 are small and visible).
- **Legacy twins:** stay frozen; fixes land in the latest variants only, unless a parity plan says otherwise.
- **Idempotency scope (1.4):** settled when 1.4 shipped (per user, 24 h TTL); see the Chunk 1 note.
- **Rate limiting (4.2):** Keycloak brute-force detection plus a documented, cost-labelled opt-in edge policy (user, 2026-11-17, re-decided after the 0.1 audit found no in-app login). A deliberate shortcut, see 4.2.
- **Prod target (6.1):** GCP (Cloud Run + Neon + Secret Manager) is the real prod target; compose stays the local and pipeline runtime.
- **Overlap with B-KC** (`backlog/p2-product-correctness/20261007-1114-keycloak-auth-followups.md`): checked by the meta-plan; B-KC holds only a deferred .NET route-table/JWKS item and no chunk 4 work. Its decision 1 (actuator/swagger stay authenticated) is carried into 4.4.

## Open questions

None. 4.2 was re-decided 2026-11-17 (Keycloak brute-force detection).
