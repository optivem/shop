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

## AT Cycle (per ticket)

_Triggered when the ticket produces change-driven AC (i.e. **change-driven AC = yes** — story or bug). Task/chore tickets do not enter the AT Cycle; see the Intake gates above._

The unit of work in the AT Cycle is the **ticket** — all change-driven AC scenarios for the ticket are batched through each phase together. AT - RED - TEST writes all scenarios at once; AT - RED - DSL, AT - RED - SYSTEM DRIVER, and AT - GREEN - SYSTEM each operate over the full set. There is no per-scenario inner loop.

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

- **DSL Interface Changed?** — Did AT - RED - TEST need to extend the DSL with new methods (and therefore add `"TODO: DSL"` prototypes)? Determined by compile failure during AT - RED - TEST - COMMIT. If no new DSL methods were needed, the answer is No.
- **External System Driver Interface Changed?** — Did AT - RED - DSL add or modify any external-system Driver interface? See `glossary.md` for the definition of *interface change*. Set as an explicit flag at the end of AT - RED - DSL - WRITE.
- **System Driver Interface Changed?** — Did AT - RED - DSL add or modify any system Driver interface? Set as an explicit flag at the end of AT - RED - DSL - WRITE.

---

## Contract Test Sub-Process

_Triggered when the AT cycle detects external driver interface changes._

Before entering CT - RED - TEST, the orchestrator runs the **External System Onboarding Sub-Process** (see below) as a prerequisite to ensure an External System Driver and an accessible Test Instance exist for the system being integrated. If the Driver already exists, Onboarding returns immediately; otherwise it provisions a dockerized stand-in, defines a minimal Driver interface and implementation, and proves it works with a single Smoke Test before CT - RED - TEST begins.

```
External System Onboarding Sub-Process (see below)
    │
    ▼
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

## External System Onboarding Sub-Process

_Triggered when the orchestrator is about to enter the Contract Test Sub-Process and needs to ensure an External System Driver and Test Instance exist for the system being integrated._

This is a one-time-per-external-system sub-process that handles the prerequisites for contract testing. It separates two orthogonal concerns from the per-scenario CT loop:

1. **Code question** — do we already have an External System Driver (interface + impl) for this system?
2. **Environment question** — do we have an accessible Test Instance to talk to (real sandbox or dockerized stand-in)?

These are independent: a Driver may exist without an accessible Test Instance, or vice versa. The sub-process resolves both before per-scenario contract testing starts.

### Steps

1. **Check whether an External System Driver exists** for the system being integrated (interface + impl under `external/`). If yes → return immediately to the Contract Test Sub-Process; skip the rest of onboarding.
2. **Check whether an External System Test Instance is accessible** (real sandbox or already-running dockerized stand-in). If yes → skip step 3.
3. **Provision a dockerized stand-in** following the json-server pattern established in `system/external-real-sim` (Node.js + json-server-based mock, runs in docker, mounted at a known port; the existing `external-real-sim` already emulates ERP, Tax, and Clock subsystems and is the reference shape for new stand-ins).
4. **Define a minimal External System Driver interface** — only the methods needed to support a single Smoke Test for this external system. Resist the urge to flesh out the full surface area; per-scenario interface growth happens in the CT loop.
5. **Implement the Driver impl just enough for one Smoke Test** to compile and run against either the real Test Instance or the dockerized stand-in.
6. **Write a single Smoke Test** for this External System.
7. **Run the Smoke Test and verify it passes.** If it fails, ask the user for support and STOP. Do NOT continue.
8. **STOP — HUMAN REVIEW.** Present the dockerized stand-in (if newly provisioned), the minimal Driver interface, the Driver impl, and the Smoke Test for approval. Do NOT continue.
9. **COMMIT** with message `External System Onboarding | <External System Name>`.
10. **Return to the Contract Test Sub-Process** at CT - RED - TEST.

The Onboarding sub-process internally handles the "Driver already exists" early return, so the Contract Test Sub-Process can refer to it unconditionally; no per-entry diamond is needed at the CT layer.

---

## Phase-to-Agent Mapping

| Phase | Agent | Notes |
|-------|-------|-------|
| Intake (story) | atdd-story | Behavioral. Change-driven AC: one scenario per acceptance criterion. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (bug) | atdd-bug | Behavioral. Change-driven AC: one scenario per distinct reproduction path (default: one). Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (task) | atdd-task | Structural. Interface change at the system boundary; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (existing AC must stay green). |
| Intake (chore) | atdd-chore | Structural. Internal-only change; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to Legacy Coverage Cycle if the ticket has a Legacy Coverage section; otherwise no cycle (existing AC must stay green). |
| AT - RED - TEST | test-agent | All scenarios for the ticket batched. WRITE = STOP (review tests). COMMIT = compile, conditional DSL-prototype STOP, run, disable, commit. |
| AT - RED - DSL | dsl-agent | WRITE = STOP (review DSL + Driver-interface-changed flags). COMMIT = conditional Driver-prototype impl, commit. |
| AT - RED - SYSTEM DRIVER | driver-agent | System Drivers only. WRITE = STOP, COMMIT = commit. |
| AT - GREEN - SYSTEM | system-agent | Single agent, full-stack (backend + frontend). One COMMIT covering all implementation. |
| CT - RED - TEST | test-agent | WRITE = STOP, COMMIT = commit + push |
| CT - RED - DSL | dsl-agent | WRITE = STOP, COMMIT = commit + push |
| CT - RED - EXTERNAL DRIVER | driver-agent | External Drivers only. WRITE = STOP, COMMIT = commit + push. |
| CT - GREEN - STUB | stub-agent | Implement External System Stubs; commit. |

## STOP Behaviour

Every WRITE phase ends with **STOP** — present results to the user and wait for explicit approval before proceeding to COMMIT. The orchestrator does not auto-approve; phase progression always requires a human decision at every STOP.

## Resume Detection

Scan for `@Disabled` annotations to determine where to resume:

| Marker | Resume at |
|--------|-----------|
| `AT - RED - TEST` | Check for `"TODO: DSL"` prototypes → if found, AT - RED - DSL; if not, AT - GREEN - SYSTEM |
| `AT - RED - DSL` | Check for `"TODO: Driver"` prototypes → if found in system drivers, AT - RED - SYSTEM DRIVER; if found in external drivers, Contract Test sub-process; otherwise AT - GREEN - SYSTEM |
| `AT - RED - SYSTEM DRIVER` | AT - GREEN - SYSTEM |
| `CT - RED - TEST` | Check for `"TODO: DSL"` prototypes → if found, CT - RED - DSL; if not, CT - GREEN - STUB |
| `CT - RED - DSL` | Check for `"TODO: Driver"` prototypes in external drivers → if found, CT - RED - EXTERNAL DRIVER; if not, CT - GREEN - STUB |
| `CT - RED - EXTERNAL DRIVER` | CT - GREEN - STUB |

## Escalation

If any agent reports it cannot proceed (stuck, unexpected pattern, test failure it cannot explain), STOP and present the blocker to the user.
