# ATDD Orchestrator

This document defines the decision flow for the ATDD pipeline. Each phase is defined in detail in `acceptance-tests.md` and `contract-tests.md` — this file controls **which phases run and in what order**.

> **Naming note**: The word *shop* appears in two distinct senses in ATDD content — `shop/` (with slash) is a package/folder convention inside the driver layer; `shop` (without slash) is the SUT repository name. See `glossary.md` for details.

## Intake (per ticket)

Before any cycle runs, the picked ticket is classified by `atdd-manager` into one of six ticket types: **story**, **bug**, **system-api-task**, **system-ui-task**, **external-api-task**, or **chore**. Classification by ticket type happens first; cycle routing is decided afterwards by two orthogonal gates (see below). See `glossary.md` for the full definitions of *behavioral change*, *structural change*, and *Legacy Coverage*.

Each of the six intake agents reads the ticket, processes the type-specific content, AND processes the **optional Legacy Coverage section** if it appears in the ticket schema:

- `atdd-story` → reads the story's acceptance criteria; produces 1+ change-driven AC scenarios (one per acceptance criterion). Behavioral.
- `atdd-bug` → reads the bug's reproduction paths; produces 1+ change-driven AC scenarios (one per distinct reproduction path; default: one). Behavioral.
- `atdd-task-system-api` → reads the System API redesign description; updates the System API Driver (interface + impl). Driver *interfaces* may grow; existing AC must keep passing through them. Single-driver scope by construction (single-boundary ticket). Produces no change-driven AC scenarios. Structural.
- `atdd-task-system-ui` → reads the System UI redesign description; updates the System UI Driver (interface + impl). Driver *interfaces* may grow; existing AC must keep passing through them. Single-driver scope by construction (single-boundary ticket). Produces no change-driven AC scenarios. Structural.
- `atdd-task-external-api` → reads the External System API change description; updates the External System Driver via the Contract Test Sub-Process (which itself wraps the External System Onboarding Sub-Process if no Driver yet exists). Single-driver scope by construction (single-boundary ticket). Produces no change-driven AC scenarios. Structural.
- `atdd-chore` → reads the structural change description; the change is internal-only (refactor a class, rename, dependency upgrade). No boundary change; drivers untouched. Produces no change-driven AC scenarios. Structural.

In addition, **all six agents** produce 0+ legacy-coverage AC scenarios from the optional Legacy Coverage section in the ticket schema (see [Legacy Coverage in glossary.md](glossary.md#legacy-coverage)).

All six agents end with **STOP** for human approval before any cycle begins.

After STOP, two **orthogonal gates** are evaluated per ticket:

1. **Ticket has a Legacy Coverage section?** — Universal; applies to all six ticket types.
   - Yes → enter the **Legacy Coverage Cycle** (test-last; retroactive AC for already-built behavior; tests should pass on first run; **not ATDD**).
   - No → skip the Legacy Coverage Cycle.
2. **Change-driven AC produced?** — Determined by ticket type: yes for story/bug, no for system-api-task / system-ui-task / external-api-task / chore. This *is* the behavioral-vs-structural distinction.
   - Yes → enter the **AT Cycle** (test-first ATDD; Red → Green per scenario).
   - No → skip the AT Cycle.

**Order when both gates fire: Legacy Coverage Cycle first, then AT Cycle.** Rationale: fill the coverage gap before piling new behavior on top.

The ten possible per-ticket flows:

- story/bug + Legacy Coverage section → Legacy Coverage Cycle → AT Cycle → DONE
- story/bug, no Legacy Coverage section → AT Cycle → DONE
- system-api-task + Legacy Coverage section → Legacy Coverage Cycle → System API Task Cycle → DONE
- system-api-task, no Legacy Coverage section → System API Task Cycle → DONE
- system-ui-task + Legacy Coverage section → Legacy Coverage Cycle → System UI Task Cycle → DONE
- system-ui-task, no Legacy Coverage section → System UI Task Cycle → DONE
- external-api-task + Legacy Coverage section → Legacy Coverage Cycle → External API Task Cycle → DONE
- external-api-task, no Legacy Coverage section → External API Task Cycle → DONE
- chore + Legacy Coverage section → Legacy Coverage Cycle → Chore Cycle → DONE
- chore, no Legacy Coverage section → Chore Cycle → DONE

The three Task Cycles and the Chore Cycle are all governed by the rule that **existing AC must stay green**; the **Acceptance Stage** of the CI pipeline at the end of each cycle is the verifier.

**Output asymmetry — change-driven AC vs legacy-coverage AC.** The two artifact streams are produced under different rules:

- **Change-driven AC** is **ticket-type-specific** — only `atdd-story` and `atdd-bug` produce it (one scenario per acceptance criterion or per distinct reproduction path). It is the input to the AT Cycle; each scenario drives one pass through AT - RED - TEST → AT - GREEN - SYSTEM (see the Scenario Loop below).
- **Legacy-coverage AC** is **universal-optional** — any ticket type may produce it, gated by whether the ticket schema carries a Legacy Coverage section. It is the input to the Legacy Coverage Cycle, which is **test-last** (retroactive tests for already-built behavior; tests should pass on first run; not ATDD).

| Ticket type | Agent | Class | Change-driven AC | Legacy-coverage AC | Routes to |
|-------------|-------|-------|------------------|--------------------|-----------|
| `story` | `atdd-story` | Behavioral | One scenario per acceptance criterion | 0+ scenarios if the ticket has a Legacy Coverage section | AT Cycle (always); Legacy Coverage Cycle if the ticket has a Legacy Coverage section (Legacy first, then AT) |
| `bug` | `atdd-bug` | Behavioral | One scenario per distinct reproduction path (default: one) | 0+ scenarios if the ticket has a Legacy Coverage section | AT Cycle (always); Legacy Coverage Cycle if the ticket has a Legacy Coverage section (Legacy first, then AT) |
| `system-api-task` | `atdd-task-system-api` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | System API Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| `system-ui-task` | `atdd-task-system-ui` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | System UI Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| `external-api-task` | `atdd-task-external-api` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | External API Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| `chore` | `atdd-chore` | Structural | None | 0+ scenarios if the ticket has a Legacy Coverage section | Chore Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |

From AT - RED - TEST onward the AT Cycle pipeline is identical regardless of which behavioral intake variant produced the scenarios. The Legacy Coverage Cycle's internal phases are TBD; see `glossary.md`.

## AT Cycle (per ticket)

_Triggered when the ticket produces change-driven AC (i.e. **change-driven AC = yes** — story or bug). Task tickets enter the matching task cycle instead — **System API Task Cycle**, **System UI Task Cycle**, or **External API Task Cycle** depending on the task subtype; chore tickets enter the **Chore Cycle** instead. See the Intake gates above and the dedicated cycle sections below._

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

## System API Task Cycle

_Triggered when ticket type = system-api-task (System API Driver redesign at the system boundary, no change-driven AC, no other boundaries touched)._

A System API task changes the System API at the boundary — request/response DTOs, endpoints, status codes, and the like. The System API Driver is updated to match. Driver *interfaces* may grow or change; existing acceptance tests must keep passing through them. Single-driver scope by construction (single-boundary ticket); multi-boundary work is split into multiple coordinated tickets at creation. The cycle ends with a **single COMMIT** covering the driver update.

The system-api-task ticket carries a **checklist of structural change items** in its body; the agent ticks them off as the work is done, and once all are ticked the issue moves to DONE.

```
Triggered: ticket type = system-api-task
    │
    ▼
Update System API Driver (interface + impl)
    │
    ▼
STOP - HUMAN REVIEW (present driver changes for approval)
    │
    ▼
COMMIT: <Ticket> | SYSTEM API REDESIGN
    │
    ▼
Wait for Pipeline - Acceptance Stage
    │
    ├── Acceptance Stage passes? ──── Yes ──→ Tick checklist items; if all ticked move issue to DONE ──→ Mark Ticket DONE ──→ DONE
    │
    No
    ▼
Fix breakage ──→ Wait for Pipeline - Acceptance Stage (loop until green)
```

The cycle is governed by the rule that **existing AC must stay green**. There is no per-scenario RED/GREEN (no new change-driven AC is produced); the verifier is the **Acceptance Stage** of the CI pipeline, which runs the existing acceptance, contract, and unit suites against the changed driver code. A red Acceptance Stage routes back through Fix → Wait until green.

---

## System UI Task Cycle

_Triggered when ticket type = system-ui-task (System UI Driver redesign at the system boundary, no change-driven AC, no other boundaries touched)._

A System UI task changes the System UI at the boundary — page structure, form fields, navigation, and the like. The System UI Driver is updated to match. Driver *interfaces* may grow or change; existing acceptance tests must keep passing through them. Single-driver scope by construction (single-boundary ticket); multi-boundary work is split into multiple coordinated tickets at creation. The cycle ends with a **single COMMIT** covering the driver update.

The system-ui-task ticket carries a **checklist of structural change items** in its body; the agent ticks them off as the work is done, and once all are ticked the issue moves to DONE.

```
Triggered: ticket type = system-ui-task
    │
    ▼
Update System UI Driver (interface + impl)
    │
    ▼
STOP - HUMAN REVIEW (present driver changes for approval)
    │
    ▼
COMMIT: <Ticket> | SYSTEM UI REDESIGN
    │
    ▼
Wait for Pipeline - Acceptance Stage
    │
    ├── Acceptance Stage passes? ──── Yes ──→ Tick checklist items; if all ticked move issue to DONE ──→ Mark Ticket DONE ──→ DONE
    │
    No
    ▼
Fix breakage ──→ Wait for Pipeline - Acceptance Stage (loop until green)
```

The cycle is governed by the rule that **existing AC must stay green**. There is no per-scenario RED/GREEN (no new change-driven AC is produced); the verifier is the **Acceptance Stage** of the CI pipeline, which runs the existing acceptance, contract, and unit suites against the changed driver code. A red Acceptance Stage routes back through Fix → Wait until green.

---

## External API Task Cycle

_Triggered when ticket type = external-api-task (an external system changed its API; we are reacting to a third-party change, no change-driven AC of our own)._

An external system updated its API — new version, breaking change, deprecated endpoint, or similar. We update the External System Driver to match the new external surface. The work routes through the **Contract Test Sub-Process** (which itself routes through the **External System Onboarding Sub-Process** if no Driver yet exists). Single-driver scope by construction (single-boundary ticket); multi-boundary work is split into multiple coordinated tickets at creation. After CT completes its four-commit sequence, the Acceptance Stage of the pipeline runs to verify nothing else broke; on red, fix-loop until green.

The external-api-task ticket carries a **checklist of structural change items** in its body; the agent ticks them off as the work is done, and once all are ticked the issue moves to DONE.

```
Triggered: ticket type = external-api-task
    │
    ▼
Contract Test Sub-Process (see above)
    │
    ▼
Wait for Pipeline - Acceptance Stage
    │
    ├── Acceptance Stage passes? ──── Yes ──→ Tick checklist items; if all ticked move issue to DONE ──→ Mark Ticket DONE ──→ DONE
    │
    No
    ▼
Fix breakage ──→ Wait for Pipeline - Acceptance Stage (loop until green)
```

There is no standalone STOP - HUMAN REVIEW or COMMIT in this cycle — those happen inside the Contract Test Sub-Process (which has its own per-phase STOPs and four-commit sequence). The cycle is governed by the rule that **existing AC must stay green**; the verifier is the **Acceptance Stage** of the CI pipeline, which runs the existing acceptance, contract, and unit suites against the changed driver code. A red Acceptance Stage routes back through Fix → Wait until green.

---

## Chore Cycle

_Triggered when ticket type = chore (internal-only structural change, drivers untouched, no change-driven AC)._

A chore changes nothing at the boundary — it's an internal refactor, rename, dependency upgrade, or similar. Drivers (interfaces and implementations) are untouched. The cycle is therefore a single-step implementation followed by review, commit, and Acceptance Stage verify.

The chore ticket carries a **checklist of refactor / upgrade steps** in its body; the agent ticks them off as the work is done, and once all are ticked the issue moves to DONE.

```
Triggered: ticket type = chore
    │
    ▼
Implement chore (refactor / upgrade / rename / etc.)
    │
    ▼
STOP - HUMAN REVIEW (present implementation for approval)
    │
    ▼
COMMIT: <Ticket> | CHORE
    │
    ▼
Wait for Pipeline - Acceptance Stage
    │
    ├── Acceptance Stage passes? ──── Yes ──→ Tick checklist items; if all ticked move issue to DONE ──→ Mark Ticket DONE ──→ DONE
    │
    No
    ▼
Fix breakage ──→ Wait for Pipeline - Acceptance Stage (loop until green)
```

Like the three Task Cycles, the Chore Cycle is governed by the rule that **existing AC must stay green**. There is no RED/GREEN per scenario; the **Acceptance Stage** of the CI pipeline is the verifier, running the existing suites against the changed code. A red Acceptance Stage routes back through Fix → Wait until green.

---

## Phase-to-Agent Mapping

| Phase | Agent | Notes |
|-------|-------|-------|
| Intake (story) | atdd-story | Behavioral. Change-driven AC: one scenario per acceptance criterion. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (bug) | atdd-bug | Behavioral. Change-driven AC: one scenario per distinct reproduction path (default: one). Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to AT Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. |
| Intake (system-api-task) | atdd-task-system-api | Structural. System API redesign at the system boundary; single-driver scope; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to System API Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| Intake (system-ui-task) | atdd-task-system-ui | Structural. System UI redesign at the system boundary; single-driver scope; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to System UI Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| Intake (external-api-task) | atdd-task-external-api | Structural. External System API change at the system boundary; single-driver scope; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to External API Task Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
| Intake (chore) | atdd-chore | Structural. Internal-only change; no change-driven AC. Optional legacy-coverage AC if the ticket has a Legacy Coverage section. STOP for approval. Routes to Chore Cycle (always); Legacy Coverage Cycle first if the ticket has a Legacy Coverage section. Governed by "existing AC must stay green"; the Acceptance Stage of the CI pipeline is the verifier. |
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

## Commit Confirmation

Every COMMIT step in every cycle is gated by the rule defined in [`commit-confirmation.md`](commit-confirmation.md): the agent must ask "Can I commit?" and receive an explicit yes before running `git commit` (or `gh issue close`, or any other GitHub state mutation). The rule lives in its own file because it is a shared, low-level gate that leaf committing agents import directly — independent of the routing flow defined here.

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
