# 20260422-0553 — Workflow Diff Plan

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-04-22T06:14:59Z`

Architecture: both
Stage: all

## Remaining items (awaiting author decision)

### DIFF-13: acceptance-stage — `CHANNEL` env value case differs between Java/.NET (uppercase) and TypeScript (lowercase)

**Stage:** acceptance-stage, acceptance-stage-legacy, acceptance-stage-cloud
**Scope:** monolith and multitier — Java/.NET vs TypeScript

**Files:**
- `.github/workflows/monolith-java-acceptance-stage.yml` — uses `-Dchannel=API` and `-Dchannel=UI` (uppercase)
- `.github/workflows/monolith-dotnet-acceptance-stage.yml` — uses `CHANNEL: API` and `CHANNEL: UI` (uppercase)
- `.github/workflows/monolith-typescript-acceptance-stage.yml` — uses `CHANNEL: api` and `CHANNEL: ui` (lowercase)
- Same pattern across `multitier-*-acceptance-stage.yml`, `*-acceptance-stage-legacy.yml`, `*-acceptance-stage-cloud.yml`.

**Details:**
`CHANNEL` is `API`/`UI` in Java and .NET but `api`/`ui` in TypeScript. If any shared tooling or log grep relies on case, it behaves differently per language.

**Recommendation:**
Standardize to uppercase `API`/`UI` across TypeScript workflows (consistent with Java/.NET and with common HTTP convention). Alternate: normalize case inside the TypeScript test harness.

---

### DIFF-14: acceptance-stage-cloud — Java monolith lacks explicit `externalSystemMode` where .NET/TS set `EXTERNAL_SYSTEM_MODE: stub`

**Stage:** acceptance-stage-cloud
**Scope:** monolith — Java vs .NET vs TypeScript

**Files:**
- `.github/workflows/monolith-java-acceptance-stage-cloud.yml:250` — `Run Acceptance Tests - API Channel` → includes `-Dchannel=API`, no `EXTERNAL_SYSTEM_MODE`
- `.github/workflows/monolith-dotnet-acceptance-stage-cloud.yml:293-297` — env block includes `EXTERNAL_SYSTEM_MODE: stub` and `CHANNEL: API`
- `.github/workflows/monolith-typescript-acceptance-stage-cloud.yml:294-299` — env block includes `EXTERNAL_SYSTEM_MODE: stub` and `CHANNEL: api`

**Details:**
Java's test harness may be inferring stub-mode from another signal or defaulting; .NET/TS set it explicitly.

**Recommendation:**
Add explicit `-DexternalSystemMode=stub` in `monolith-java-acceptance-stage-cloud.yml` for the acceptance-* jobs so intent is visible in the workflow, matching .NET/TS.
