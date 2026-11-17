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

## ▶ Next executable step (resume here)

Reviewed 2026-11-17 (meta-plan U3): items SHOW and the cloud deploy already delivered are deleted below. This runs as meta-plan unit U9 (Lane B), chunk order 0 → 1 → 3 → 4 → 2 → 5 → 6; 5.4 and 6.3 touch `_prod-stage.yml` and wait until B-QAS (U5) is pushed. **Next: Step 0.1**: for each finding listed in 0.1, grep the six backends (`system/monolith/{java,dotnet,typescript}`, `system/multitier/backend-{java,dotnet,typescript}`; `backend-clean-java` where it has the code) and write the finding-by-language table at the top of this plan. Plan edit only, no code. Then 1.7.

## Steps

Each chunk is independently executable, and the chunks can be done in any order. Within a chunk, do Java first (including the multitier backends), then .NET, then TS. After each chunk run the pre-commit verification from CLAUDE.md (`./compile-all.sh`, then `--sample` system tests per affected language).

### Chunk 0 — Parity audit (cheap, do first)
- [ ] 0.1 Check whether each remaining finding applies in `system/monolith/{java,dotnet,typescript}` and `system/multitier/backend-{java,dotnet,typescript}`: actuator/health exposure (Java still has `show-details: always`), dev settings with no prod profile, secret defaults, missing security headers / CORS policy, Swagger exposure, no rate limit, trace id missing from ProblemDetail. Chunk 1 (transactions, coupon race, versioning, idempotency, pagination) and gateway resilience already landed in all six backends, so they are out of the audit. Record a finding-by-language table at the top of this plan, so the later chunks only touch what is actually broken.

### Chunk 1 — Data correctness (ATDD scenarios)
Steps 1.1–1.6 are done (2026-11-03); the child plan `20261009-1545-production-readiness-chunk1-data-correctness.md` is deleted. The decisions (idempotency per user with a 24 h TTL, 422 on a reused key with a different body, optional 1-based `page`/`size` with default 50 and cap 100, `LIKE '%x%'` search accepted as is) live in the code and its tests.
- [ ] 1.7 Replace Lombok `@Data` on JPA entities with explicit id-based `equals`/`hashCode` (Java only: the `core/entities` in `system/monolith/java` and `system/multitier/backend-java`, and the `*JpaEntity` classes in `backend-clean-java`).
- [ ] 1.8 Scheduled purge of expired `idempotency_keys` rows (older than 24 h). Today an expired row is only replaced on its next use; this is an ops item.

### Chunk 2 — Resilience
2.1 (shared pooled client, configurable timeouts) and 2.2 (GET-only retry + circuit breaker) were delivered by SHOW: `GatewayHttpClient` + `ResilienceSettings` (Resilience4j) in the three Java backends, `Api/Resilience/ResilienceExtensions.cs` in both .NET backends, `resilience.ts` in both TS backends, with `ErpGatewayResilience*` tests.
- [ ] 2.3 Decide on clock: keep it as a test seam only, or use a local `Clock` bean in prod with the external clock reserved for non-prod. Record the decision.

### Chunk 3 — Observability and config profiles
3.1 (metrics), the trace-propagation and log parts of 3.2, and 3.3 (structured JSON logs) were delivered by SHOW: OTel/Micrometer pushed over OTLP (`OtlpExportEnvironmentPostProcessor`, `micrometer-registry-otlp`; metrics are not exposed over HTTP, Java actuator exposes only `health,info`), JSON logs with `trace_id`/`span_id` always on (`logging.structured.format.console: logstash`, `TraceJsonConsoleFormatter`, `logger.ts` / `json-logger.ts`), viewable through the opt-in `otel-lgtm` compose profile.
- [ ] 3.2 Trace id in ProblemDetail responses (what is left of 3.2): add the current `trace_id` to every ProblemDetail body (e.g. Java `GlobalExceptionHandler` sets only `timestamp`/`errors` today), so a user-reported error can be found in the logs.
- [ ] 3.4 Actuator lockdown: `health` details only for authorized callers, liveness/readiness probes split, and the Docker `HEALTHCHECK` pointed at liveness.
- [ ] 3.5 `application-prod` profile: `show-sql: false`, `thymeleaf.cache: true`, and similar dev-only flags off. Wire the profile into the docker/compose pipeline configs.

### Chunk 4 — Security hardening
- [ ] 4.1 Fail fast at startup when `AUTH_CLIENT_SECRET`, the DB password or the issuer URI are missing or equal to the test defaults (prod profile only, so local and pipeline stacks keep working).
- [ ] 4.2 Rate limiting: a minimal in-app login-attempt limit in all six backends, plus a `docs/operations/` how-to for an opt-in edge policy (Cloud Armor rule or a free fronting proxy) with its monthly cost labelled, not wired into the default deploy (decided, see Resolved decisions). **Shortcut, noted:** this is not real edge rate limiting. Cloud Run limits nothing by default and the in-app limit is per instance, so a determined attacker spread across instances isn't stopped; we accept that to keep the default $0 for students who clone shop.
- [ ] 4.3 Security headers (CSP, HSTS, `X-Content-Type-Options`, frame options, referrer policy) and an explicit CORS policy.
- [ ] 4.4 Swagger/OpenAPI exposure policy: disabled or admin-only in prod. Keep B-KC decision 1 (actuator/swagger on backend-java stay authenticated, never public).

### Chunk 5 — Supply chain and repo hygiene
5.1 (grouped Dependabot for gradle, nuget, npm, docker, github-actions: `.github/dependabot.yml`), CodeQL (`codeql.yml`), 5.3 (Trivy, CRITICAL/HIGH with a fix, report-only by design, in the standalone `supply-chain.yml`) and the SBOM part of 5.4 (Syft CycloneDX in `supply-chain.yml` and `release-sbom.yml`, BuildKit `sbom: true` + `provenance: mode=max` in the commit stages) were delivered by SHOW.
- [ ] 5.2 Secret scanning and push protection (what is left of 5.2): both are **disabled** on `optivem/shop` (`gh api repos/optivem/shop --jq .security_and_analysis`, 2026-11-17). Enable them in the repo settings (free for public repos) and note it in `SECURITY.md` (5.5).
- [ ] 5.4 Image signing with cosign (keyless, GitHub OIDC) in the prod stage (what is left of 5.4). Touches `_prod-stage.yml`: after B-QAS (Conflict 4).
- [ ] 5.5 Add `SECURITY.md` and `CODEOWNERS`.
- [ ] 5.6 Pin base images by digest and tighten the floating `^` ranges in TS, supported by a lockfile and renovation PRs.

### Chunk 6 — Deploy and operations
6.1 was delivered by the cloud deploy (`terraform/main.tf`: project, WIF, service accounts, Secret Manager; `terraform/neon.sh` for Neon; Cloud Run services and `--min-instances`/`--max-instances` set by `_deploy-stage.yml`). 6.2 is in `CONTRIBUTING.md` (rollback-safety table and expand-contract worked examples). The k6 half of 6.7 is `load-test.yml` (SHOW).
- [ ] 6.3 Rollback procedure for the prod stage, and a documented rollback test. Touches `_prod-stage.yml`: after B-QAS (Conflict 4).
- [ ] 6.4 Runbooks under `docs/operations/` (new; `docker/docs/operations/` no longer exists and docs live under `docs/`): deploy, rollback, DB restore, dependency outage.
- [ ] 6.5 Postgres (Neon) backups with a restore drill.
- [ ] 6.6 SLOs plus alert rules built on the OTel metrics from SHOW.
- [ ] 6.7 DAST (ZAP baseline) as an optional, opt-in workflow, like `load-test.yml` (what is left of 6.7).

## Resolved decisions

Decided (user, 2026-11-16, meta-plan G0 answer 7, plus the meta-plan's U9 chunk order):

- **Scope:** shop only. Chunks 5–6 apply to the shop itself, not to the repos gh-optivem scaffolds; scaffolding is a separate later decision.
- **Order of chunks:** 0, 1, 3, 4, 2, 5, 6 (Chunk 0 fixes the scope; 1 is the real correctness risk; 3 and 4 are small and visible).
- **Legacy twins:** stay frozen; fixes land in the latest variants only, unless a parity plan says otherwise.
- **Idempotency scope (1.4):** settled when 1.4 shipped (per user, 24 h TTL); see the Chunk 1 note.
- **Rate limiting (4.2):** in-code login limit only; the edge policy is a documented, cost-labelled opt-in (user, 2026-11-17). A deliberate shortcut, see 4.2.
- **Prod target (6.1):** GCP (Cloud Run + Neon + Secret Manager) is the real prod target; compose stays the local and pipeline runtime.
- **Overlap with B-KC** (`backlog/p2-product-correctness/20261007-1114-keycloak-auth-followups.md`): checked by the meta-plan; B-KC holds only a deferred .NET route-table/JWKS item and no chunk 4 work. Its decision 1 (actuator/swagger stay authenticated) is carried into 4.4.

## Open questions

None. The edge rate-limit mechanism (4.2) was decided 2026-11-17.
