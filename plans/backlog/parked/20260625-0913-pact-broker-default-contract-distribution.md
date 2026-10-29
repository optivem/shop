# 2026-06-25 09:13 — OSS Pact Broker as the **default** contract-distribution mechanism

> **Deferred — scope blocked by infrastructure constraint (2026-06-25):**
> An ephemeral OSS Pact Broker (Docker Compose, per CI run) works for **monorepo** because consumer
> publish and provider verify happen inside the same run. It **cannot work for multi-repo**: each
> provider's CI run is a separate process on a separate machine, potentially hours or days after the
> consumer published — the ephemeral broker from that run is already gone. Multi-repo broker-as-default
> therefore requires a **persistent, network-reachable broker** (standing infra someone hosts, patches,
> and authenticates). Until the hosting question is resolved (teacher-hosted VPS, per-student account,
> or other), this plan is deferred. The current file-based default remains in place for both monorepo
> and multi-repo.

> **Supersedes the decision in** `contracts/README.md` (and the resolved-then-deleted plan
> `20260624-0814-multirepo-contract-transport-dockerized-broker.md`), which chose **file-based** as the
> $0 default and the broker as a documented opt-in. This plan **reverses that**: the self-hosted OSS
> **Pact Broker** (`pactfoundation/pact-broker` + Postgres) becomes the default contract transport for
> both providers and the consumer, across monorepo **and** multi-repo. The file-based `contracts/` path
> becomes the documented fallback (or is removed — see OQ-5).
>
> **Related work (do not fold in):**
> - **gh-optivem file-based fix** (already done, separate): the `.cs` provider-verification path was not
>   being flattened on scaffold (`FixupDotnetSourceFiles` / `dotnetContractsPathReplacements`, 7→5 `../`).
>   That fix keeps the *current* file-based default green; this plan replaces that default. The path-rewrite
>   machinery (`copyContracts`, `contractsPathReplacements`, `dotnetContractsPathReplacements`) is retired
>   here only if OQ-5 removes the fallback.
> - **Deferred sibling** `plans/deferred/20260616-0830-component-pact-layer-other-multitier-backends.md`
>   (mirror the opt-in component/Pact layer into dotnet+ts backends) — different concern; leave deferred.

## TL;DR

**Why:** Today contract distribution is file-based. In a **monorepo** the consumer writes
`contracts/frontend-backend.json` and the providers read it from the same filesystem — trivial. In
**multi-repo** the only $0 option is *consumer-CI-push* (the consumer's CI commits the regenerated pact
into the provider repo). Both work but neither teaches the real-world tool: a **Pact Broker** with
versioned pacts, `can-i-deploy`, the compatibility matrix, and webhooks. Per the decision recorded in this
plan, the teaching project should make the **OSS broker the default** so students learn the production
workflow end-to-end, not the $0 stand-ins.

**End result:** One self-hosted OSS Pact Broker (Docker) is the single source of truth for the
`frontend → backend` contract. The **consumer publishes** its pact to the broker; **all three providers**
(`backend-java`, `backend-dotnet`, `backend-typescript`) **verify against the broker** and **publish
verification results**; deployment is gated by **`can-i-deploy`**. `contracts/README.md` is rewritten to
teach the broker as the default and explain the persistent-vs-ephemeral trade-off.

## The crux to settle first — persistent vs ephemeral broker (OQ-1)

Broker-as-default has one hard consequence that shapes everything else:

- **Monorepo:** consumer + providers run in the **same** CI workflow, so an **ephemeral** broker
  (`docker compose up pact-broker` for the duration of the run) is sufficient and $0/zero-standing-infra.
- **Multi-repo:** the consumer publishes in the **frontend repo's** CI run and each provider verifies in a
  **different repo's** CI run, minutes-to-days apart. They share **no process and no filesystem**. A
  per-run ephemeral broker therefore **cannot work** — multi-repo as-default **requires a persistent,
  network-reachable, authenticated broker** that outlives any single CI run.

So "broker as default" for multi-repo is a decision to introduce **standing infrastructure** (someone
hosts, patches, backs up, and authenticates a broker). This contradicts the "don't propagate cost/infra to
students" rule and must be chosen with eyes open. OQ-1 decides who hosts it; the rest of the plan assumes
that decision.

## Outcomes

- A `docker-compose.pact-broker.yml` (or service added to the existing per-flavor compose) running
  `pactfoundation/pact-broker` + Postgres, with seed/healthcheck wiring.
- Consumer (`frontend-react`) **publishes** the union pact to the broker (`PACT_BROKER_BASE_URL`,
  versioned by git SHA + branch) instead of writing `../contracts`.
- Providers verify **from the broker** and **publish results**:
  - `backend-java`: `@PactBroker` (replacing `@PactFolder("../../../contracts")`).
  - `backend-dotnet`: `.WithPactBrokerSource(...)` (replacing `.WithFileSource(...)` — the 7→5 path
    rewrite becomes moot).
  - `backend-typescript`: `@pact-foundation/pact` broker source (replacing the file source).
- `gh optivem component-test setup/run --suite provider-verification` understands a broker source
  (env-driven), so the commit-stage wiring stays a one-liner.
- `can-i-deploy` gate added to the commit/release stage(s).
- `contracts/README.md` rewritten: broker = default; persistent-vs-ephemeral explained; file-based framed
  as fallback (or removed per OQ-5).
- gh-optivem scaffolds the broker (compose + CI env + `can-i-deploy`) into generated repos.

## ▶ Next executable step (resume here)

Decision/design plan — **no mechanical edits until OQ-1 and OQ-5 are settled.** Run `/refine-plan` on this
file to resolve the Open Questions (especially **who hosts the persistent multi-repo broker** and **whether
the file-based fallback is kept or removed**), then `/execute-plan`. First executable unit once settled:
**Step 1** (stand up the broker via docker-compose and prove publish→verify→can-i-deploy locally in the
monorepo).

## Steps

- [ ] **Step 1 — Broker up + local proof (monorepo, ephemeral).** Add `pactfoundation/pact-broker` +
  Postgres to compose with a healthcheck. Locally: consumer publishes the union pact, one provider verifies
  from the broker and publishes the result, `can-i-deploy` passes. Record the exact env vars
  (`PACT_BROKER_BASE_URL`, auth, version/branch/tag).
- [ ] **Step 2 — Consumer publishes to the broker.** Switch `frontend-react`'s pact tests / `test:pact`
  to publish (pact-js publisher or `pact-broker publish`) keyed by git SHA + branch. Stop writing
  `../contracts` (gated by OQ-5).
- [ ] **Step 3 — Provider verification from the broker (all three backends).** Replace the file source in
  `backend-java` (`@PactBroker`), `backend-dotnet` (`.WithPactBrokerSource`), and `backend-typescript`
  (broker source); enable **publishing verification results**. Keep the in-process host / Testcontainers /
  WireMock harness unchanged — only the pact *source* changes.
- [ ] **Step 4 — `gh optivem component-test` broker source.** Teach `component-test setup`/`run
  --suite provider-verification` to pass broker env (URL + auth + selectors) so each commit-stage workflow
  stays a single `component-test run` call. No per-language special-casing in the workflows.
- [ ] **Step 5 — CI wiring (commit/acceptance stages).** Monorepo: bring the broker up in-run (ephemeral).
  Multi-repo: point CI at the **persistent** broker from OQ-1; add the **`can-i-deploy`** gate before
  deploy/promotion. Pass the broker token as a CI secret.
- [ ] **Step 6 — `can-i-deploy` lesson + matrix.** Wire `can-i-deploy --to-environment` so a provider
  cannot be promoted against a contract it fails; surface the compatibility matrix. This is the headline
  payoff that justifies the broker over the file stand-ins.
- [ ] **Step 7 — Docs.** Rewrite `contracts/README.md`: broker = default; ephemeral (monorepo) vs
  persistent (multi-repo) trade-off; auth setup; file-based fallback status per OQ-5. Update each tier's
  README "Optional: component & contract tests" section.
- [ ] **Step 8 — gh-optivem scaffolding follow-on.** Scaffold the broker compose + CI env + `can-i-deploy`
  into generated repos. Per OQ-5, either retire the file-copy path (`copyContracts`,
  `contractsPathReplacements`, `dotnetContractsPathReplacements`, `FixupDotnetSourceFiles`) or keep it as
  the fallback behind a flag. Cross-reference this plan from a gh-optivem plan if the scaffolder change is
  large enough to stand alone.

## Open questions

Each carries a recommendation (`→`).

- **OQ-1 — Who hosts the persistent multi-repo broker?** central teacher-hosted broker · per-student
  managed/cloud broker · monorepo-ephemeral-only (multi-repo stays file-based).
  → **Decide explicitly during `/refine-plan`.** Recommended framing: a **single small teacher-hosted
  persistent broker** (one `docker compose`/managed Postgres) that all student repos publish/verify
  against, with per-student auth tokens — it's the only shape where multi-repo `can-i-deploy` is real. Call
  out the cost/ownership honestly; this is the price of broker-as-default.
- **OQ-2 — Ephemeral broker for monorepo, or always the persistent one?**
  → **Recommend ephemeral for monorepo** (zero standing infra, same-run publish→verify) and persistent only
  where multi-repo forces it — one mechanism documented, two deployment modes.
- **OQ-3 — Broker auth model.** bearer token · basic auth · read-all/write-scoped.
  → **Recommend a write-scoped token** for CI publish + a read token for verification, both CI secrets;
  documented as the one setup step the default now requires.
- **OQ-4 — Pact versioning + selectors.** git SHA + branch; consumer version selectors (`mainBranch`,
  `deployedOrReleased`).
  → **Recommend SHA-as-version + branch tag**, providers verify `latest from main` + `deployed`; this is
  what makes `can-i-deploy` meaningful.
- **OQ-5 — Keep the file-based fallback, or remove it?**
  → **Recommend keep as a documented, flag-gated fallback** for one cycle (so a broker outage doesn't brick
  the teaching build), then remove once the broker default is proven green across all combos. Removing it
  immediately deletes the just-fixed `.cs` path-rewrite machinery — defer that deletion to a follow-up.
- **OQ-6 — `can-i-deploy` at which stage?** commit · acceptance · release.
  → **Recommend release/promotion gate** (commit stage publishes + verifies; the deploy gate is where
  `can-i-deploy` belongs conceptually).

## Verification (operator / human — not agent steps)

- Re-run both originally-failing combos end-to-end after migration:
  `gh-optivem` jobs `…dotnet ts java` monorepo + multirepo (the runs that exposed the file-path bug).
- Confirm a deliberate consumer-side breaking change makes the provider's verification **fail** against the
  broker (proving the broker isn't a false-green), and that `can-i-deploy` blocks promotion.
- Confirm the monorepo ephemeral path needs **no** standing infra; confirm multi-repo points at the
  persistent broker from OQ-1.

## Out of scope

- The component-test harness internals (in-process host, Testcontainers-Postgres, WireMock stubs) — only
  the pact **source** changes, not the harness.
- The `contract` → `provider-verification` suite naming (already settled).
- PactFlow / hosted-SaaS adoption — OSS self-hosted only.
- The deferred dotnet+ts component-layer mirroring plan (its own file; stays deferred).
