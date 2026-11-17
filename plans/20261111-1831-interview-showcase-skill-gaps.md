# 2026-11-11 18:31:52 UTC — Interview showcase: close the skill gaps in shop

## TL;DR

**Why:** Shop is going to be the portfolio piece for interviews, framed for a full-stack/tech-lead role. It already covers pipelines, ATDD, contract tests, three languages and auth, but it's missing several things interviewers routinely probe for, especially observability, resilience, security scanning and performance.
**End result:** Shop shows the most-asked-about skills that it doesn't cover today, each as an opt-in addition that costs $0 by default. A `docs/tour.md` walks an interviewer through every skill with direct file links. The **interview-ready milestone** (tour + supply-chain security) ships first; observability, resilience, k6 and the parity sweep follow. Messaging/outbox goes to its own plan.
**Unchanged:** the default clone and pipeline stay $0 and zero-infra. New tooling either runs in CI on free public-GitHub features (Dependabot, CodeQL, Trivy, SBOM) or sits behind an opt-in switch (the `observability` Compose profile, the k6 `workflow_dispatch` workflow, nightly Stryker). Existing system tests, meta-stages and the production deploy keep working as they do today. Every language-level change lands in Java, .NET and TypeScript.

## Outcomes

- An interviewer can be walked through a 10–15 minute guided tour (`docs/tour.md`) that maps each skill to a concrete file, test or pipeline run.
- **Observability:** one request can be traced end to end (frontend → backend → ERP) with OpenTelemetry, and logs are structured and correlated by trace ID. This runs locally with the single `grafana/otel-lgtm` container behind a Docker Compose profile (opt-in).
- **Resilience:** ERP and Clock calls have timeouts, retries and a circuit breaker (Resilience4j / Polly / TS equivalent), and an acceptance test proves the behaviour when the ERP stub is slow or down.
- **Supply-chain security:** dependency updates (Dependabot, grouped), CodeQL, a Trivy container scan and an SBOM are generated in CI, all free on public GitHub.
- **Performance:** a k6 load test against the local Docker stack, with an opt-in workflow that publishes p95 latency.
- Parity gaps are closed so "same design in three languages" holds up when probed: OpenAPI for .NET/TS and mutation testing (Stryker) for .NET/TS.
- **Messaging:** a separate plan exists for a Postgres-backed transactional outbox (no broker). Frontend testing (#8) has its own plan; feature flags (#7) were dropped from the inventory.

## Already demonstrated (no work needed — reference in the tour)

CI/CD meta-stages (commit → acceptance → RC → release), ATDD with DSL/driver/client layers, contract tests (Pact + ErpRealContractTest), backend + frontend in Java/.NET/TypeScript, monolith + multitier, clean-architecture variant, Keycloak/OIDC, Postgres + Flyway, Terraform + GCP Cloud Run, SonarCloud, PITest mutation testing (Java), OpenAPI (Java monolith), idempotency, Docker Compose, production deploy at shop.optivem.com.

## Gap inventory (priority order)

| # | Gap | Interview value | Effort | Recommendation |
|---|---|---|---|---|
| 1 | `docs/tour.md` interview walkthrough | Very high | Low | **Do first** |
| 2 | Observability (OTel tracing, structured logs, metrics) | Very high | Medium | **Do** |
| 3 | Resilience on outbound calls (timeout/retry/circuit breaker) + test | High | Low–Medium | **Do** |
| 4 | Supply-chain security (Dependabot, CodeQL, Trivy, SBOM) | High | Low | **Do** |
| 5 | Performance/load test (k6) | Medium–High | Low | Do next |
| 6 | Async messaging + outbox pattern (e.g. order-placed event) | High (backend/system-design interviews) | High | **Spun off:** `plans/20261112-0939-transactional-outbox-postgres.md` (Postgres-backed outbox, no broker) |
| 8 | Frontend testing beyond e2e (component tests, axe a11y, Lighthouse CI) | Medium (frontend roles) | Medium | **Spun off:** `plans/20261112-0939-frontend-testing-component-a11y-lighthouse.md` |
| 9 | OpenAPI parity (.NET, TS) | Low–Medium | Low | Fold into parity sweep |
| 10 | Mutation-testing parity (Stryker .NET/TS) | Low–Medium | Low | Fold into parity sweep |
| 11 | Caching (Redis) | Low–Medium | Medium | Skip unless a real use case appears |
| 12 | Kubernetes/Helm | Medium (platform roles) | High | Skip; Cloud Run already shows container deploy |

## ▶ Next executable step (resume here)

**Testing phase done (2026-11-17, unit U1 of meta-plan `20261116-2003`).** Nothing is left to verify. The orchestrator deletes this plan after the user's MUI visual check.
- Step 3: the resilience tests (`PlaceOrderNegativeIsolatedTest` fail fast when the ERP is down or slow, UC-001.14) passed on all six stacks in the local Phase B run (monolith and multitier × Java, .NET, TS). No breaker state leaked into the following isolated tests, the TS SamplingBreaker included (REL #60: no leak seen).
- Step 5: the local docker k6 smoke against the Java monolith stub stack sent 17 requests, 0% failed. The first `load-test` dispatch (run 44047318333) failed with 33% errors: the stub stack had no `/erp/api/promotion` mapping, so every place order got a 503. Fixed in `c1000142` (static `erp-promotion.json`). Rerun 44049789149 is green: 440 requests, 0% failed, p95 browse 33.6 ms, view 24.0 ms, place order 81.3 ms.
- Step 6: a signed-in `GET /v3/api-docs` answers 200 on the Java monolith, the .NET monolith and backend, the Next.js monolith and the NestJS backend (anonymous callers get a redirect or 401). The Java backends answered 500 (springdoc 2.6.0 against Spring 6.2); fixed in `c1000142` (springdoc 2.8.9). NestJS `authorization.component.spec.ts` passes in the commit stage (run 43952886881). Stryker: the four `*-mutation-nightly.yml` workflows run green every night and upload their report artifacts (e.g. 43842873631, 43846054957, 43843171039, 43846123170).
- Step 4b: `release-sbom.yml` by `workflow_dispatch` (run 44047311148, tag `monolith-java-v1.0.238`) attached `monolith-system-java.cdx.json` to the release. It still doesn't fire on its own (REL #47, deferred).

Step 7 is done: the outbox and frontend-testing plans are `plans/20261112-0939-transactional-outbox-postgres.md` and `plans/20261112-0939-frontend-testing-component-a11y-lighthouse.md` (both need `/refine-plan` first); feature flags (#7) are dropped.

Step 8 is done: `docs/tour.md` has skills-map rows for Resilience and Performance (k6), the OpenAPI and mutation-testing rows now cover all three languages, the supply-chain row mentions the release SBOMs, and the "Coming next" section is gone (everything in it landed).

**No coding or testing left.**

## Steps

Every step is code-complete and verified (see ▶ Next).


## Constraints

- Templates propagate cost to cloning students: every addition must be **opt-in, $0 and zero-infra by default** (Compose profiles, `workflow_dispatch`, nightly jobs; no paid SaaS on the default path).
- Every language change lands in Java, .NET and TypeScript (latest + legacy where applicable) to preserve parity.
- System tests must stay green (`--sample` per language before each commit).

## Resolved decisions

- **Interview target role:** full-stack/tech-lead. Shop already spans frontend, backend, CI/CD and ATDD, so this framing makes the most of what exists. The gap-inventory order stays as written.
- **Messaging (#6):** pursue it in a **separate plan** using a Postgres-backed transactional outbox with an in-process relay, so there's no broker and no extra infra by default. That shows the pattern (atomic write + at-least-once delivery) within the $0 constraint. Step 7 spins off that plan rather than deciding whether to do it.
- **Renovate vs Dependabot:** **Dependabot**, with grouped updates (`groups:` per ecosystem in `.github/dependabot.yml`). It's built into GitHub, so it works zero-install for cloning students. Renovate needs the Mend app installed on each repo, and a cloned `renovate.json` does nothing without it. Grouping keeps PR noise low.
- **Observability backend:** a single **`grafana/otel-lgtm`** container in the opt-in `observability` Compose profile. One OTLP endpoint feeds traces (Tempo), metrics and logs (Loki), all correlated in Grafana with one command. Jaeger alone would cover only traces.
- **Feature flags (#7) / frontend testing (#8):** drop #7 (little interview value over what already exists); spin off #8 into its own plan, since it fits the full-stack/tech-lead framing. Decided 2026-11-11.
- **Resilience behaviour (Step 3):** follow industry practice. Put a tight timeout on every outbound call. Retry only idempotent calls (the ERP product GET), 2–3 attempts with exponential backoff and jitter. A circuit breaker fails fast while the ERP is down. Place-order then returns `503 Service Unavailable` with `Retry-After` and a readable problem+json message. **No fallback** to cached or default prices, because charging a guessed price is a money-correctness bug (fallbacks belong on read paths only). Client retries are safe because place-order is already idempotent. Acceptance tests cover a slow ERP stub (fails within the timeout with the 503 message) and a down ERP stub (the breaker opens and later calls fail immediately). Decided 2026-11-11.
- **Timeline:** the first interview is soon, so **Steps 1 + 4 ship first** as an "interview-ready" milestone. Both are cheap, need no system-test churn, and make the repo presentable quickly. After that the order is 2 → 3 → 5 → 6 → 7, with Step 8 running alongside each.
