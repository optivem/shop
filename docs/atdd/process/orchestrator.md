# ATDD Orchestrator

This document defines the decision flow for the ATDD pipeline. Each phase is defined in detail in `acceptance-tests.md` and `contract-tests.md` — this file controls **which phases run and in what order**.

> **Naming note**: The word *shop* appears in two distinct senses in ATDD content — `shop/` (with slash) is a package/folder convention inside the driver layer; `shop` (without slash) is the SUT repository name. See `glossary.md` for details.

## Intake (per ticket)

Before any cycle runs, the picked ticket is classified by `atdd-manager` into one of four ticket types: **story**, **bug**, **task**, or **chore**. Classification by ticket type happens first; cycle routing is decided afterwards by two orthogonal gates (see below). See `glossary.md` for the full definitions of *behavioral change*, *structural change*, and *Legacy Coverage*.

Each of the four intake agents reads the ticket, processes the type-specific content, AND processes the **optional Legacy Coverage section** if it appears in the ticket schema:

- `atdd-story` → reads the story's acceptance criteria; produces 1+ change-driven AC scenarios (one per acceptance criterion). Behavioral.
- `atdd-bug` → reads the bug's reproduction paths; produces 1+ change-driven AC scenarios (one per distinct reproduction path; default: one). Behavioral.
- `atdd-task` → reads the structural change description; the change is at the system boundary (system API, system UI, external system API). Driver *implementations* update to match the new interface; driver *interfaces* stay the same so existing acceptance tests still pass through them. Produces no change-driven AC scenarios. Structural.
- `atdd-chore` → reads the structural change description; the change is internal-only (refactor a class, rename, dependency upgrade). No boundary change; drivers untouched. Produces no change-driven AC scenarios. Structural.

In addition, **all four agents** produce 0+ legacy-coverage AC scenarios from the optional Legacy Coverage section in the ticket schema (see [Legacy Coverage in glossary.md](glossary.md#legacy-coverage)).

All four agents end with **STOP** for human approval before any cycle begins.

After STOP, two **orthogonal gates** are evaluated per ticket:

1. **Ticket has a Legacy Coverage section?** — Universal; applies to all four ticket types.
   - Yes → enter the **Legacy Coverage Cycle** (test-last; retroactive AC for already-built behavior; tests should pass on first run; **not ATDD**).
   - No → skip the Legacy Coverage Cycle.
2. **Change-driven AC produced?** — Determined by ticket type: yes for story/bug, no for task/chore. This *is* the behavioral-vs-structural distinction.
   - Yes → enter the **AT Cycle** (test-first ATDD; Red → Green per scenario).
   - No → skip the AT Cycle.

**Order when both gates fire: Legacy Coverage Cycle first, then AT Cycle.** Rationale: fill the coverage gap before piling new behavior on top.

The four possible per-ticket flows:

- story/bug + Legacy Coverage section → Legacy Coverage Cycle → AT Cycle → DONE
- story/bug, no Legacy Coverage section → AT Cycle → DONE
- task/chore + Legacy Coverage section → Legacy Coverage Cycle → DONE
- task/chore, no Legacy Coverage section → DONE (the structural change itself is plain code work governed by "existing AC stay green" — not a cycle)

**Output asymmetry — change-driven AC vs legacy-coverage AC.** The two artifact streams are produced under different rules:

- **Change-driven AC** is **ticket-type-specific** — only `atdd-story` and `atdd-bug` produce it (one scenario per acceptance criterion or per distinct reproduction path). It is the input to the AT Cycle; each scenario drives one pass through AT - RED - TEST → AT - GREEN - SYSTEM (see the Scenario Loop below).
- **Legacy-coverage AC** is **universal-optional** — any ticket type may produce it, gated by whether the ticket schema carries a Legacy Coverage section. It is the input to the Legacy Coverage Cycle, which is **test-last** (retroactive tests for already-built behavior; tests should pass on first run; not ATDD).

| Ticket type | Agent | Class | Change-driven AC | Legacy-coverage AC | Routes to |
|-------------|-------|-------|------------------|--------------------|-----------|
| `story` | `atdd-story` | Behavioral | One scenario per acceptance criterion | 0+ scenarios if the ticket has a Legacy Coverage section | AT Cycle (always); Legacy Coverage Cycle if the ticket has a Legacy Coverage section (Legacy first, then AT) |
| `bug` | `atdd-bug` | Behavioral | One scenario per distinct reproduction path (default: one) | 0+ scenarios if the ticket has a Legacy Coverage section | AT Cycle (always); Legacy Coverage Cycle if the ticket has a Legacy Coverage section (Legacy first, then AT) |
| `task` | `atdd-task` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (structural change is plain code work governed by existing AC staying green) |
| `chore` | `atdd-chore` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (structural change is plain code work governed by existing AC staying green) |

From AT - RED - TEST onward the AT Cycle pipeline is identical regardless of which behavioral intake variant produced the scenarios. The Legacy Coverage Cycle's internal phases are TBD; see `glossary.md`.

## AT Cycle (per scenario)

```
AT - RED - TEST
    │
    ├── DSL Interface Changed? ──── No ──→ AT - GREEN - SYSTEM
    │
    Yes
    ▼
AT - RED - DSL
    │
    ├── External System Driver Interface Changed? ──── Yes ──→ Contract Test Sub-Process (see below)
    │                                                                │
    │                                                                ▼
    │                                                          (then continue ↓)
    │
    ├── System Driver Interface Changed? ──── No ──→ AT - GREEN - SYSTEM
    │
    Yes
    ▼
AT - RED - SYSTEM DRIVER
    │
    ▼
AT - GREEN - SYSTEM
```

### Decision criteria

- **DSL Interface Changed?** — Did AT - RED - TEST - COMMIT add any "TODO: DSL" stubs to DSL interfaces? If no new DSL methods were needed, the answer is No.
- **External System Driver Interface Changed?** — Did AT - RED - DSL add or modify interfaces under `external/` (e.g. `driver-port/.../external/clock`, `driver-port/.../external/erp`)? See `glossary.md` for the definition of *interface change*.
- **System Driver Interface Changed?** — Did AT - RED - DSL add or modify interfaces under `shop/` (e.g. `driver-port/.../shop/api`, `driver-port/.../shop/ui`)? If no new driver methods were needed in shop/, the answer is No.

---

## Contract Test Sub-Process

_Triggered when the AT cycle detects external driver interface changes._

```
CT - RED - TEST
    │
    ├── DSL Interface Changed? ──── No ──→ CT - GREEN - STUB
    │
    Yes
    ▼
CT - RED - DSL
    │
    ├── External System Driver Interface Changed? ──── No ──→ CT - GREEN - STUB
    │
    Yes
    ▼
CT - RED - EXTERNAL DRIVER
    │
    ▼
CT - GREEN - STUB
```

After the contract test sub-process completes, return to the AT cycle and continue with the system driver check.

---

## Scenario Loop

The AT cycle repeats for each scenario in the ticket:

1. Run the AT cycle for the first scenario (or the first scenario that needs new DSL).
2. After AT - GREEN - SYSTEM, if there are remaining `// TODO:` scenarios in the test file, loop back to AT - RED - TEST for the next scenario.
3. Continue until all scenarios are GREEN.

---

## Phase-to-Agent Mapping

| Phase | Agent | Notes |
|-------|-------|-------|
| Intake (story) | atdd-story | Behavioral. Change-driven AC: one scenario per acceptance criterion. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (bug) | atdd-bug | Behavioral. Change-driven AC: one scenario per distinct reproduction path (default: one). Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (task) | atdd-task | Structural. Interface change at the system boundary; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (existing AC must stay green). |
| Intake (chore) | atdd-chore | Structural. Internal-only change; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (existing AC must stay green). |
| AT - RED - TEST | test-agent | WRITE = STOP, COMMIT = commit + push |
| AT - RED - DSL | dsl-agent | WRITE = STOP, COMMIT = commit + push |
| AT - RED - SYSTEM DRIVER | driver-agent | WRITE = STOP, COMMIT = commit + push. Only `shop/` drivers. |
| AT - GREEN - SYSTEM | backend-agent + frontend-agent + release-agent | Backend first, then frontend, then release commit |
| CT - RED - TEST | test-agent | WRITE = STOP, COMMIT = commit + push |
| CT - RED - DSL | dsl-agent | WRITE = STOP, COMMIT = commit + push |
| CT - RED - EXTERNAL DRIVER | driver-agent | WRITE = STOP, COMMIT = commit + push. Only `external/` drivers. |
| CT - GREEN - STUB | backend-agent + release-agent | Implement stubs, then release commit |

## STOP Behaviour

Every WRITE phase ends with **STOP** — present results to the user and wait for approval before proceeding to COMMIT.

- **Normal mode:** Wait for explicit human approval.
- **Autonomous mode (`--autonomous`):** Auto-approve and proceed immediately.

## Resume Detection

Scan for `@Disabled` annotations to determine where to resume:

| Marker | Resume at |
|--------|-----------|
| `AT - RED - TEST` | Check for TODO: DSL stubs → if found, AT - RED - DSL; if not, AT - GREEN - SYSTEM |
| `AT - RED - DSL` | Check for TODO: Driver stubs → if found in `shop/`, AT - RED - SYSTEM DRIVER; if not, AT - GREEN - SYSTEM |
| `AT - RED - SYSTEM DRIVER` | AT - GREEN - SYSTEM |
| `CT - RED - TEST` | Check for TODO: DSL stubs → if found, CT - RED - DSL; if not, CT - GREEN - STUB |
| `CT - RED - DSL` | Check for TODO: Driver stubs in `external/` → if found, CT - RED - EXTERNAL DRIVER; if not, CT - GREEN - STUB |
| `CT - RED - EXTERNAL DRIVER` | CT - GREEN - STUB |

## Escalation

If any agent reports it cannot proceed (stuck, unexpected pattern, test failure it cannot explain), STOP and present the blocker to the user — **even in autonomous mode**.
