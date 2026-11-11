# 2026-11-11 18:31:52 UTC — Interview showcase: close the skill gaps in shop

## TL;DR

**Why:** Shop is going to be the portfolio piece for interviews. It already covers pipelines, ATDD, contract tests, three languages and auth, but it's missing several things interviewers routinely probe for, especially observability, resilience, security scanning and performance.
**End result:** Shop shows the most-asked-about skills that it doesn't cover today, each as an opt-in addition that costs $0 by default. A `docs/tour.md` walks an interviewer through every skill with direct file links.

## Outcomes

- An interviewer can be walked through a 10–15 minute guided tour (`docs/tour.md`) that maps each skill to a concrete file, test or pipeline run.
- **Observability:** one request can be traced end to end (frontend → backend → ERP) with OpenTelemetry, and logs are structured and correlated by trace ID. This runs locally with the Jaeger/Grafana containers behind a Docker Compose profile (opt-in).
- **Resilience:** ERP and Clock calls have timeouts, retries and a circuit breaker (Resilience4j / Polly / TS equivalent), and an acceptance test proves the behaviour when the ERP stub is slow or down.
- **Supply-chain security:** dependency updates (Renovate or Dependabot), CodeQL, a Trivy container scan and an SBOM are generated in CI, all free on public GitHub.
- **Performance:** a k6 load test against the local Docker stack, with an opt-in workflow that publishes p95 latency.
- Parity gaps are closed so "same design in three languages" holds up when probed: OpenAPI for .NET/TS and mutation testing (Stryker) for .NET/TS.

## Already demonstrated (no work needed — reference in the tour)

CI/CD meta-stages (commit → acceptance → RC → release), ATDD with DSL/driver/client layers, contract tests (Pact + ErpRealContractTest), backend + frontend in Java/.NET/TypeScript, monolith + multitier, clean-architecture variant, Keycloak/OIDC, Postgres + Flyway, Terraform + GCP Cloud Run, SonarCloud, PITest mutation testing (Java), OpenAPI (Java monolith), idempotency, Docker Compose, production deploy at shop.optivem.com.

## Gap inventory (priority order)

| # | Gap | Interview value | Effort | Recommendation |
|---|---|---|---|---|
| 1 | `docs/tour.md` interview walkthrough | Very high | Low | **Do first** |
| 2 | Observability (OTel tracing, structured logs, metrics) | Very high | Medium | **Do** |
| 3 | Resilience on outbound calls (timeout/retry/circuit breaker) + test | High | Low–Medium | **Do** |
| 4 | Supply-chain security (Renovate, CodeQL, Trivy, SBOM) | High | Low | **Do** |
| 5 | Performance/load test (k6) | Medium–High | Low | Do next |
| 6 | Async messaging + outbox pattern (e.g. order-placed event) | High (backend/system-design interviews) | High | Consider; needs its own plan |
| 7 | Feature flags / dark launch | Medium | Low–Medium | Optional |
| 8 | Frontend testing beyond e2e (component tests, axe a11y, Lighthouse CI) | Medium (frontend roles) | Medium | Optional |
| 9 | OpenAPI parity (.NET, TS) | Low–Medium | Low | Fold into parity sweep |
| 10 | Mutation-testing parity (Stryker .NET/TS) | Low–Medium | Low | Fold into parity sweep |
| 11 | Caching (Redis) | Low–Medium | Medium | Skip unless a real use case appears |
| 12 | Kubernetes/Helm | Medium (platform roles) | High | Skip; Cloud Run already shows container deploy |

## ▶ Next executable step (resume here)

Step 1: write `docs/tour.md`. Pick one use case (Place Order) and trace it through the spec (`docs/shared/specs/use-cases/`), the acceptance test, the scenario DSL, the API and UI drivers, the ERP stub and contract test, and the `meta-prerelease-stage` workflow, using real relative links throughout. Add a "Skills map" table (skill → file/run) and link the tour from the top of `README.md`. Gate: the user reviews the tour before Step 2 starts. This needs no code changes and gives the interview narrative everything else plugs into.

## Steps

- [ ] Step 1: Write `docs/tour.md` (guided path + skills map) and link it from `README.md`.
- [ ] Step 2: Observability. Add OTel instrumentation to all backends + frontend, emit structured JSON logs with trace IDs, and put a `observability` Docker Compose profile (Jaeger/Grafana-LGTM) behind opt-in. Off by default; no cloud cost.
- [ ] Step 3: Resilience. Wrap the ERP/Clock clients with timeout + retry + circuit breaker in each language, and add an acceptance test that drives a slow/failing ERP stub and asserts graceful behaviour.
- [ ] Step 4: Supply-chain security. Add Renovate (or Dependabot), CodeQL, a Trivy image scan in the commit stage, and SBOM generation (Syft/CycloneDX) attached to releases.
- [ ] Step 5: Performance. Add a k6 script for browse + place order against the local stack, plus an opt-in `workflow_dispatch` workflow that reports p95/p99.
- [ ] Step 6: Parity sweep. OpenAPI for the .NET and TS backends, and Stryker mutation testing for .NET and TS (nightly, matching the Java PITest job).
- [ ] Step 7: Decide on #6–#8 (messaging/outbox, feature flags, frontend testing). Either spin off separate plans or drop them.
- [ ] Step 8: Update `docs/tour.md` skills map with each addition as it lands.

## Constraints

- Templates propagate cost to cloning students: every addition must be **opt-in, $0 and zero-infra by default** (Compose profiles, `workflow_dispatch`, nightly jobs; no paid SaaS on the default path).
- Every language change lands in Java, .NET and TypeScript (latest + legacy where applicable) to preserve parity.
- System tests must stay green (`--sample` per language before each commit).

## Open questions

- **Interview target role:** backend, full-stack, platform/DevOps or test-automation/QA lead? This reorders #6–#12. *Recommended: assume full-stack/tech-lead and keep the order above.*
- **Messaging (#6):** worth the effort? It's the most common system-design probe shop can't answer today, but it adds a broker to the stack. *Recommended: separate plan, with an in-process/Postgres-backed outbox so there's zero extra infra by default.*
- **Renovate vs Dependabot:** *Recommended: Renovate, because it groups updates across the three language ecosystems and keeps PR noise low.*
- **Observability backend:** *Recommended: the single `grafana/otel-lgtm` container for one-command local traces, metrics and logs.*
- **Timeline:** when is the first interview? It decides whether only Step 1 (+ Step 4, which is cheap) ships first.
