# 20260422-0553 — Workflow Diff Plan

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-04-22T06:14:59Z`

Architecture: both
Stage: all

## Remaining items (awaiting author decision)

### DIFF-8: acceptance-stage-legacy — same `Compile System Tests` / `Build Test Project` / missing pattern as DIFF-7

**Stage:** acceptance-stage-legacy
**Scope:** monolith and multitier — Java vs .NET vs TypeScript

**Files (monolith):**
- `.github/workflows/monolith-java-acceptance-stage-legacy.yml:163-165` — `Compile System Tests` present
- `.github/workflows/monolith-dotnet-acceptance-stage-legacy.yml:153-155` — `Build Test Project` present
- `.github/workflows/monolith-typescript-acceptance-stage-legacy.yml` — no equivalent step

**Files (multitier):**
- `.github/workflows/multitier-java-acceptance-stage-legacy.yml:163-165` — `Compile System Tests` present
- `.github/workflows/multitier-dotnet-acceptance-stage-legacy.yml:153-155` — `Build Test Project` present
- `.github/workflows/multitier-typescript-acceptance-stage-legacy.yml` — no equivalent step

**Details:**
Identical shape to DIFF-7 but in the legacy variant.

**Recommendation:**
Apply the same fixes as DIFF-7 to the four legacy workflow files.

VJ: Let's call it Compile System Tests... also in TypeScript, can we add compilation? should we?
(→ VJ already approved the `Compile System Tests` rename; TypeScript compile question resolved via DIFF-7 → `tsc --noEmit`)

---

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
