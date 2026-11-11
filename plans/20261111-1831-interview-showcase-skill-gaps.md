# 2026-11-11 18:31:52 UTC — Interview showcase: close the skill gaps in shop

> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-11-11T18:42:58Z`

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
- **Messaging:** a separate plan exists for a Postgres-backed transactional outbox (no broker). Feature flags (#7) and frontend testing (#8) are either spun off or explicitly dropped.

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
| 6 | Async messaging + outbox pattern (e.g. order-placed event) | High (backend/system-design interviews) | High | **Separate plan** (Postgres-backed outbox, no broker) |
| 7 | Feature flags / dark launch | Medium | Low–Medium | Optional |
| 8 | Frontend testing beyond e2e (component tests, axe a11y, Lighthouse CI) | Medium (frontend roles) | Medium | Optional |
| 9 | OpenAPI parity (.NET, TS) | Low–Medium | Low | Fold into parity sweep |
| 10 | Mutation-testing parity (Stryker .NET/TS) | Low–Medium | Low | Fold into parity sweep |
| 11 | Caching (Redis) | Low–Medium | Medium | Skip unless a real use case appears |
| 12 | Kubernetes/Helm | Medium (platform roles) | High | Skip; Cloud Run already shows container deploy |

## ▶ Next executable step (resume here)

The interview-ready milestone (Steps 1 + 4) is done. Next is Step 2, observability: add OpenTelemetry instrumentation to every backend (Java monolith + multitier + clean-java, .NET, TS) and the React frontend, emit structured JSON logs carrying trace IDs, and add an opt-in `observability` Docker Compose profile with one `grafana/otel-lgtm` container, kept identical across the compose files that `compose-drift.yml` checks. Gate: `./compile-all.sh` is green and `gh optivem system-test run --sample` passes for each language (**ask the user before running system tests locally**). Then add an Observability row to the `docs/tour.md` skills map (Step 8).

## Steps

Execution order: 2 → 3 → 5 → 6 → 7 (the interview-ready milestone, Steps 1 + 4, is done). Step 8 runs alongside each step.

- [ ] Step 2: Observability. Add OTel instrumentation to all backends + frontend, emit structured JSON logs with trace IDs, and put a `observability` Docker Compose profile (single `grafana/otel-lgtm` container) behind opt-in. Off by default; no cloud cost.
- [ ] Step 3: Resilience. Wrap the ERP/Clock clients with timeout + retry + circuit breaker in each language, and add an acceptance test that drives a slow/failing ERP stub and asserts graceful behaviour.
- [ ] Step 4b: Attach CycloneDX SBOMs to GitHub releases. The SBOMs are currently only workflow artifacts from `supply-chain.yml`; extending the gated `_prod-stage.yml` release job was judged unsafe. Recommendation: a separate workflow on `release: published` that runs `anchore/sbom-action` against the released GHCR image tags and uploads the SBOMs as release assets. Low priority, because GHCR images already carry SBOM attestations (`sbom: true`).
- [ ] Step 5: Performance. Add a k6 script for browse + place order against the local stack, plus an opt-in `workflow_dispatch` workflow that reports p95/p99.
- [ ] Step 6: Parity sweep. OpenAPI for the .NET and TS backends, and Stryker mutation testing for .NET and TS (nightly, matching the Java PITest job).
- [ ] Step 7: Spin off a separate plan for #6 (Postgres-backed outbox, no broker). Decide on #7–#8 (feature flags, frontend testing): either spin off separate plans or drop them.
- [ ] Step 8: Update `docs/tour.md` skills map with each addition as it lands.

## Constraints

- Templates propagate cost to cloning students: every addition must be **opt-in, $0 and zero-infra by default** (Compose profiles, `workflow_dispatch`, nightly jobs; no paid SaaS on the default path).
- Every language change lands in Java, .NET and TypeScript (latest + legacy where applicable) to preserve parity.
- System tests must stay green (`--sample` per language before each commit).

## Resolved decisions

- **Interview target role:** full-stack/tech-lead. Shop already spans frontend, backend, CI/CD and ATDD, so this framing makes the most of what exists. The gap-inventory order stays as written.
- **Messaging (#6):** pursue it in a **separate plan** using a Postgres-backed transactional outbox with an in-process relay, so there's no broker and no extra infra by default. That shows the pattern (atomic write + at-least-once delivery) within the $0 constraint. Step 7 spins off that plan rather than deciding whether to do it.
- **Renovate vs Dependabot:** **Dependabot**, with grouped updates (`groups:` per ecosystem in `.github/dependabot.yml`). It's built into GitHub, so it works zero-install for cloning students. Renovate needs the Mend app installed on each repo, and a cloned `renovate.json` does nothing without it. Grouping keeps PR noise low.
- **Observability backend:** a single **`grafana/otel-lgtm`** container in the opt-in `observability` Compose profile. One OTLP endpoint feeds traces (Tempo), metrics and logs (Loki), all correlated in Grafana with one command. Jaeger alone would cover only traces.
- **Timeline:** the first interview is soon, so **Steps 1 + 4 ship first** as an "interview-ready" milestone. Both are cheap, need no system-test churn, and make the repo presentable quickly. After that the order is 2 → 3 → 5 → 6 → 7, with Step 8 running alongside each.
