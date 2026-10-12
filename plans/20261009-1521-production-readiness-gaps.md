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

Chunk 1, step 1.1: confirm in Java that `OrderService.placeOrder` has no `@Transactional` (it should span `orderRepository.save` and `couponService.incrementUsageCount`). Write a failing integration test first (the repo has `integrationTest` and Testcontainers). The test makes the coupon increment fail and asserts that no order row is persisted. Then add `@Transactional`. Gate: `./gradlew build` and `./gradlew integrationTest` pass in `system/monolith/java`. This unblocks 1.2 (atomic coupon increment). Run the 1.1 check against the multitier Java backend too, since it has its own service.

## Steps

Each chunk is independently executable, and the chunks can be done in any order. Within a chunk, do Java first (including the multitier backends), then .NET, then TS. After each chunk run the pre-commit verification from CLAUDE.md (`./compile-all.sh`, then `--sample` system tests per affected language).

### Chunk 0 — Parity audit (cheap, do first)
- [ ] 0.1 Check whether each Java finding also applies in `system/monolith/{dotnet,typescript}` and `system/multitier/backend-{java,dotnet,typescript}` (transactions, coupon race, versioning, idempotency, pagination, gateway resilience, actuator/health exposure, secret defaults, dev settings). Record a finding-by-language table at the top of this plan, so the later chunks only touch what is actually broken.

### Chunk 1 — Data correctness (ATDD scenarios)
Child plan: `20261009-1545-production-readiness-chunk1-data-correctness.md` (detailed steps live there).
- [ ] 1.1 `@Transactional` on `placeOrder` (and equivalents in .NET/TS: a unit of work or DB transaction around the order save and coupon increment).
- [ ] 1.2 Atomic coupon usage increment: `UPDATE coupons SET used_count = used_count + 1 WHERE code = ? AND (usage_limit IS NULL OR used_count < usage_limit)`, and reject the order when no row is updated. Add a concurrency acceptance scenario ("concurrent orders can't exceed the coupon limit").
- [ ] 1.3 Optimistic locking: add a `version` column through a Flyway migration (expand-only) and `@Version` on `Order`. Map the conflict to a 409 ProblemDetail.
- [ ] 1.4 Idempotency: an `Idempotency-Key` header on `POST /api/orders`, a keyed table with a unique constraint, and replay of the stored response. Decide the TTL and scope (per user).
- [ ] 1.5 Pagination (page, size, stable sort) on order history and coupon list, plus an index or search strategy for order-number search (drop the leading wildcard or use `pg_trgm`). Note the DTO and test-kit impact: request DTOs stay string-only.
- [ ] 1.6 Order-number safety: confirm `generateOrderNumber` is collision-safe and not guessable. Confirm that `/orders/{n}` enforces the owner check for non-admin users.
- [ ] 1.7 Replace Lombok `@Data` on JPA entities with explicit id-based `equals`/`hashCode` (Java only).

### Chunk 2 — Resilience
- [ ] 2.1 One shared, pooled HTTP client bean (timeouts configurable, not hardcoded 10s) instead of a client per gateway call. Add the .NET `HttpClientFactory` and the TS equivalent.
- [ ] 2.2 Retry (idempotent GETs only) plus circuit breaker on the ERP, tax and clock gateways (Resilience4j or equivalent). Define the failure behaviour for each dependency and add stub-driven acceptance scenarios (stubs already exist).
- [ ] 2.3 Decide on clock: keep it as a test seam only, or use a local `Clock` bean in prod with the external clock reserved for non-prod. Record the decision.

### Chunk 3 — Observability and config profiles
- [ ] 3.1 Micrometer plus Prometheus endpoint (and the .NET/TS equivalents); expose metrics only on a management port or behind auth.
- [ ] 3.2 Request/correlation ID and trace propagation (OpenTelemetry), included in logs and in ProblemDetail responses.
- [ ] 3.3 Structured JSON logs for the `prod` profile.
- [ ] 3.4 Actuator lockdown: `health` details only for authorized callers, liveness/readiness probes split, and the Docker `HEALTHCHECK` pointed at liveness.
- [ ] 3.5 `application-prod` profile: `show-sql: false`, `thymeleaf.cache: true`, and similar dev-only flags off. Wire the profile into the docker/compose pipeline configs.

### Chunk 4 — Security hardening
- [ ] 4.1 Fail fast at startup when `AUTH_CLIENT_SECRET`, the DB password or the issuer URI are missing or equal to the test defaults (prod profile only, so local and pipeline stacks keep working).
- [ ] 4.2 Rate limiting on the API and on login paths (Bucket4j or a gateway-level policy); decide where the limit lives.
- [ ] 4.3 Security headers (CSP, HSTS, `X-Content-Type-Options`, frame options, referrer policy) and an explicit CORS policy.
- [ ] 4.4 Swagger/OpenAPI exposure policy: disabled or admin-only in prod.

### Chunk 5 — Supply chain and repo hygiene
- [ ] 5.1 Dependabot (or Renovate) for gradle, npm, nuget, docker, github-actions. Group the updates so they don't flood the monorepo.
- [ ] 5.2 CodeQL plus secret scanning and push protection.
- [ ] 5.3 Trivy scan of built images in the commit stage; define a failure threshold.
- [ ] 5.4 SBOM generation and image signing (cosign) in the prod stage.
- [ ] 5.5 Add `SECURITY.md` and `CODEOWNERS`.
- [ ] 5.6 Pin base images by digest and tighten the floating `^` ranges in TS, supported by a lockfile and renovation PRs.

### Chunk 6 — Deploy and operations
- [ ] 6.1 Terraform: Cloud Run services, Cloud SQL (Postgres), Secret Manager wiring, and minimum/maximum instances (verify what `terraform/main.tf` already has beyond the first 80 lines).
- [ ] 6.2 Migration policy doc: expand/contract, backward-compatible migrations, no destructive migration in the same release as the code that stops using the column.
- [ ] 6.3 Rollback procedure for the prod stage, and a documented rollback test.
- [ ] 6.4 Runbooks under `docker/docs/operations/` (currently empty): deploy, rollback, DB restore, dependency outage.
- [ ] 6.5 Postgres backups with a restore drill.
- [ ] 6.6 SLOs plus alert rules built on the chunk 3 metrics.
- [ ] 6.7 Load test (k6 or Gatling) and DAST (ZAP) as an optional pipeline stage.

## Open questions

- **Scope of the product** (recommended: treat the shop as a teaching reference). Should chunks 5–6 apply to the shop itself or to scaffolded student repos too? Recommended: shop only at first, then decide on scaffolding.
- **Order of chunks** (recommended: 0, 1, 3, 4, 2, 5, 6). Chunk 0 first because it fixes the scope; chunk 1 is the real correctness risk and the best teaching material; 3 and 4 are small and visible.
- **Chunk 1 and the legacy twins.** The repo has `-legacy` configs and workflows. Should the legacy variants also get fixes, or stay frozen as is? Recommended: leave legacy frozen unless a parity plan says otherwise.
- **Idempotency scope (1.4):** per user, with what TTL? Needs a decision before implementation.
- **Rate limiting (4.2):** in-app or in front of the app (Cloud Run / API gateway)? Recommended: in front, with only a minimal in-app limit for login.
- **Item 6.1** assumes GCP as the real prod target. Confirm, or confirm that compose is intentionally the runtime for a teaching repo.
- **Existing plans overlap?** `plans/20261007-1114-keycloak-auth-followups.md` may overlap with chunk 4. Run `/coordinate-plans` or check before starting chunk 4.
