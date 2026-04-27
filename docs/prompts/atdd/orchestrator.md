# ATDD Orchestrator

This document defines the decision flow for the ATDD pipeline. Each phase is defined in detail in `acceptance-tests.md` and `contract-tests.md` — this file controls **which phases run and in what order**.

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
