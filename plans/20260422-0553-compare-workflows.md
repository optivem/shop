# 20260422-0553 — Workflow Diff Plan

> 🤖 **Picked up by agent** — `ValentinaLaptop` at `2026-04-22T06:14:59Z`

Architecture: both
Stage: all

## Remaining items (awaiting author decision)

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
