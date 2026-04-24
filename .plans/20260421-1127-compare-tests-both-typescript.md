# 20260421-1127 — TypeScript System Test Alignment Plan (both)

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-21T10:09:59Z`

Reference report: [20260421-1127-compare-tests-both.md](../reports/20260421-1127-compare-tests-both.md)

Timestamp: 20260421-1127
Mode: both
Reference implementation: **Java** (align TS to Java unless explicitly stated otherwise).

All action items below respect the **Known Language-Specific Divergences (Exceptions)** list in the `compare-tests` agent spec:
- TS plural `errors/` folders for driver-port DTOs (vs Java/.NET singular `error/`) — accepted, not flagged.
- kebab-case filenames for client/service modules — accepted, not flagged.
- Two-file scenario context split (`scenario-context.ts` + `app-context.ts`) — accepted.
- Playwright fixtures / `Base*Test.ts` helper modules instead of abstract classes — accepted.
- Contract tests via `registerXxxContractTests(test)` helpers — accepted.
- `then-failure-and.ts` + inline `ThenFailureAnd` class — accepted.
- Absence of `then-success-and.ts` / `ThenSuccessAnd` — accepted.
- `ScenarioDsl.close()` method on TS only — accepted.
- `common/dtos.ts` barrel re-export file — accepted.

---

## 6. Architecture — Scenario DSL

### 6.1 Restructure `core/scenario/then/` into per-entity files — ⏳ Deferred

**Reason for deferral:** The current plan item used Java as the sole reference. Author (VJ) requested:
1. Update the `compare-tests` agent spec so that for async aspects of the Scenario DSL (Gherkin), .NET is used as reference (async-first) rather than Java (sync).
2. Re-run the comparison with all three languages in scope for `scenario/then/` specifically, and rewrite this item based on .NET's async-adapted shape as well as Java's structure.

**Action before re-execution:**
- Amend `.claude/agents/compare-tests.md` (or its referenced docs) to call out .NET as the async reference for Scenario DSL.
- Re-invoke the compare-tests agent for the scenario/then/ area.
- Replace this item with the refined instructions before executing.

### 6.2 Add `BaseGivenStep`, `BaseThenStep`, `BaseWhenStep` base classes — ⏳ Deferred (blocked by 6.1)

**Reason for deferral:**
- `BaseThenStep` references `ThenOrderImpl`, `ThenClockImpl`, `ThenCouponImpl`, etc. — the extracted step classes from 6.1. Since 6.1 is deferred, `BaseThenStep` cannot be ported cleanly yet.
- `BaseGivenStep` / `BaseWhenStep` could be done in isolation but would introduce Java-style inheritance into TS's currently composition-based step design. The target pattern should be decided together with 6.1 after the .NET comparison.

**Action before re-execution:**
- Resolve 6.1 first, then re-scope 6.2 using the post-comparison target pattern.
