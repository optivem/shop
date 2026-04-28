# Glossary

## Behavioral Change

A **behavioral change** is a change defined by **change-driven acceptance criteria** — the new (or restored) behavior IS specified by the AC scenarios produced from the ticket. Stories (`atdd-story`, new behavior) and bugs (`atdd-bug`, restored behavior) are behavioral; their change-driven AC route to the **AT Cycle** (test-first / ATDD). The unit of work in the AT Cycle is the **ticket** — all change-driven scenarios for the ticket are batched through each phase together, with no per-scenario inner loop.

Note: a behavioral-change ticket may *also* include a Legacy Coverage section; that's orthogonal — see [Legacy Coverage](#legacy-coverage) below.

## Structural Change

A **structural change** is a change that produces **no change-driven acceptance criteria**. Tasks (`atdd-task`, interface change at the system boundary) and chores (`atdd-chore`, internal-only change) are structural. Existing AC must stay green by construction, but the structural change itself is plain code work, not a cycle.

Note: a structural-change ticket may *also* include a Legacy Coverage section; that's orthogonal — see [Legacy Coverage](#legacy-coverage) below.

## Legacy Coverage

**Legacy Coverage** is orthogonal to behavioral/structural classification. It is a **section in the ticket schema**, optional on any ticket type (story, bug, task, or chore). The section lists retroactive AC scenarios for previously uncovered functionality the change touches.

Legacy Coverage uses the **test-last** approach: tests are written retroactively for already-built behavior, and they should pass on first run because the behavior already exists. **This is NOT ATDD** — there is no Red → Green per scenario. A ticket whose schema carries a Legacy Coverage section routes through the **Legacy Coverage Cycle**, regardless of ticket type.

When a ticket carries both a change-driven payload (story/bug AC, or task/chore structural change) *and* a Legacy Coverage section, the Legacy Coverage Cycle runs first, then the AT Cycle (if applicable) — fill the coverage gap before piling new behavior on top.

## Interface Change

An **interface change** is any modification to a public contract between layers. This includes:

- Adding, removing, or renaming interface methods
- Changing method signatures (parameters, return types)
- Adding, removing, or renaming fields in request or response DTOs associated with those methods

This definition applies uniformly to DSL port interfaces, Driver port interfaces, and external system interfaces.

In the intake-classification sense (`atdd-task`), an **interface change** is specifically a change at the **system boundary** — system API, system UI, or external system API. Driver *implementations* update to match the new interface; driver *interfaces* stay the same so existing acceptance tests still pass through them.

**Why it matters for the ATDD pipeline:**
- A DSL interface change → update DSL port and implementation
- A Driver interface change → update driver port and adapters
- An external system interface change (any change under `driver-port/.../external/`) → triggers the contract test subprocess (see `contract-tests.md`)
- An interface change at the system boundary classified as `atdd-task` → no cycle by default (existing AC must stay green; driver bodies adapt to the new boundary interface). Routes to the Legacy Coverage Cycle only if the ticket additionally carries a Legacy Coverage section.

## Internal-only Change

An **internal-only change** is a change inside the system that does not modify any boundary — no system API, system UI, or external system API change. Examples: refactor a class, rename, dependency upgrade. Drivers are untouched. Internal-only changes are classified as `atdd-chore`; they do not enter a cycle by default (existing AC must stay green). Routes to the Legacy Coverage Cycle only if the ticket additionally carries a Legacy Coverage section.

## Legacy Coverage Cycle

The **Legacy Coverage Cycle** is the **test-last retroactive-AC cycle**. It is reachable from any ticket type (`atdd-story`, `atdd-bug`, `atdd-task`, `atdd-chore`) whose ticket carries a [Legacy Coverage](#legacy-coverage) section. Because the behavior already exists, the retroactive acceptance tests written in this cycle should pass on first run; this is **not ATDD** (no Red → Green per scenario).

Task/chore tickets without a Legacy Coverage section do not enter any cycle — their structural change is governed only by the rule that **existing AC must stay green**. The cycle's internal phases are TBD.

## `shop/` Package vs `shop` Repository

ATDD content uses the word **shop** in two distinct ways. They look similar but mean different things:

- **`shop/` (with trailing slash)** — a package/folder convention inside the testkit's driver layer (e.g. `driver-port/.../shop/api`, `driver-adapter/.../shop/ui`). This is the SUT-internal driver namespace, paired with `external/` (drivers for external systems). The name is part of ATDD doctrine and is **not** the repo name. Do not rename it.
- **`shop` (no slash, used in repo context)** — the repository name of the system under test. When `gh optivem atdd install` copies these prompts into a student repo, this gets substituted with the student's repo name. The `shop/` package convention stays unchanged.

The two uses are kept textually distinct (slash vs. no-slash) so the install-time substitution can target one without disturbing the other.
