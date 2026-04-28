# Glossary

## Behavioral Change

A **behavioral change** is a change defined by **change-driven acceptance criteria** — the new (or restored) behavior IS specified by the AC scenarios produced from the ticket. Stories (`atdd-story`, new behavior) and bugs (`atdd-bug`, restored behavior) are behavioral; their change-driven AC route to the **AT Cycle** (test-first / ATDD). The unit of work in the AT Cycle is the **ticket** — all change-driven scenarios for the ticket are batched through each phase together, with no per-scenario inner loop.

Note: a behavioral-change ticket may *also* include a Legacy Coverage section; that's orthogonal — see [Legacy Coverage](#legacy-coverage) below.

## Structural Change

A **structural change** is a change that produces **no change-driven acceptance criteria**. The three task subtypes (`atdd-task-system-api`, `atdd-task-system-ui`, `atdd-task-external-api`, each an interface change at a single system boundary) and chores (`atdd-chore`, internal-only change) are structural. Existing AC must stay green by construction. The structural change still flows through a cycle — each task subtype enters its dedicated cycle (the **System API Task Cycle**, **System UI Task Cycle**, or **External API Task Cycle**), and chores enter the **Chore Cycle** — but each cycle has no RED/GREEN per scenario; instead it consists of implementation, STOP - HUMAN REVIEW, COMMIT, and CI pipeline verify (the pipeline is the verifier that "existing AC must stay green"). The External API Task Cycle has no standalone STOP/COMMIT — those happen inside the Contract Test Sub-Process it wraps.

Note: a structural-change ticket may *also* include a Legacy Coverage section; that's orthogonal — see [Legacy Coverage](#legacy-coverage) below.

## Legacy Coverage

**Legacy Coverage** is orthogonal to behavioral/structural classification. It is a **section in the ticket schema**, optional on any ticket type (story, bug, system-api-task, system-ui-task, external-api-task, or chore). The section lists retroactive AC scenarios for previously uncovered functionality the change touches.

Legacy Coverage uses the **test-last** approach: tests are written retroactively for already-built behavior, and they should pass on first run because the behavior already exists. **This is NOT ATDD** — there is no Red → Green per scenario. A ticket whose schema carries a Legacy Coverage section routes through the **Legacy Coverage Cycle**, regardless of ticket type.

When a ticket carries both a change-driven payload (story/bug AC, or a structural change from any of the three task subtypes or a chore) *and* a Legacy Coverage section, the Legacy Coverage Cycle runs first, then the AT Cycle (if applicable) — fill the coverage gap before piling new behavior on top.

## Interface Change

An **interface change** is any modification to a public contract between layers. This includes:

- Adding, removing, or renaming interface methods
- Changing method signatures (parameters, return types)
- Adding, removing, or renaming fields in request or response DTOs associated with those methods

This definition applies uniformly to DSL port interfaces, Driver port interfaces, and external system interfaces.

In the intake-classification sense, an **interface change** is specifically a change at the **system boundary** — system API, system UI, or external system API. The three task subtypes (`atdd-task-system-api`, `atdd-task-system-ui`, `atdd-task-external-api`) each cover exactly one of these three boundaries. Driver *implementations* update to match the new interface; driver *interfaces* stay the same so existing acceptance tests still pass through them. Single-driver scope is enforced at ticket-creation time — multi-boundary work is split into multiple coordinated tickets.

**Why it matters for the ATDD pipeline:**
- A DSL interface change → update DSL port and implementation
- A Driver interface change → update driver port and adapters
- An external system interface change (any change under `driver-port/.../external/`) → triggers the contract test subprocess (see `contract-tests.md`)
- A System API change classified as `atdd-task-system-api` → routes to the **System API Task Cycle** (update System API Driver; STOP - HUMAN REVIEW → COMMIT → CI pipeline verify). Single-driver scope.
- A System UI change classified as `atdd-task-system-ui` → routes to the **System UI Task Cycle** (update System UI Driver; STOP - HUMAN REVIEW → COMMIT → CI pipeline verify). Single-driver scope.
- An External System API change classified as `atdd-task-external-api` → routes to the **External API Task Cycle**, which wraps the Contract Test Sub-Process (per-phase RED/GREEN inside CT, four-commit sequence; the External API Task Cycle itself has no standalone STOP/COMMIT). Single-driver scope.

For all three task subtypes, existing AC must stay green; driver bodies adapt to the new boundary interface, and the CI pipeline is the verifier. If the ticket additionally carries a Legacy Coverage section, the Legacy Coverage Cycle runs first.

## Internal-only Change

An **internal-only change** is a change inside the system that does not modify any boundary — no system API, system UI, or external system API change. Examples: refactor a class, rename, dependency upgrade. Drivers are untouched. Internal-only changes are classified as `atdd-chore`; they route to the **Chore Cycle** (Implement → STOP - HUMAN REVIEW → COMMIT → CI pipeline verify, with a fix-loop on red). Existing AC must stay green; the CI pipeline is the verifier. If the ticket additionally carries a Legacy Coverage section, the Legacy Coverage Cycle runs first.

## Legacy Coverage Cycle

The **Legacy Coverage Cycle** is the **test-last retroactive-AC cycle**. It is reachable from any ticket type (`atdd-story`, `atdd-bug`, `atdd-task-system-api`, `atdd-task-system-ui`, `atdd-task-external-api`, `atdd-chore`) whose ticket carries a [Legacy Coverage](#legacy-coverage) section. Because the behavior already exists, the retroactive acceptance tests written in this cycle should pass on first run; this is **not ATDD** (no Red → Green per scenario).

Task tickets enter the matching task cycle — `system-api-task` → **System API Task Cycle**, `system-ui-task` → **System UI Task Cycle**, `external-api-task` → **External API Task Cycle** — and chore tickets enter the **Chore Cycle**. All four are structural cycles with no RED/GREEN per scenario, governed by the rule that **existing AC must stay green** (the CI pipeline at the end of each cycle is the verifier). All four cycles' phases are now defined; see `orchestrator.md` and `process-diagram.md` for the full flows. The Legacy Coverage Cycle's own internal phases are TBD.

## `shop/` Package vs `shop` Repository

ATDD content uses the word **shop** in two distinct ways. They look similar but mean different things:

- **`shop/` (with trailing slash)** — a package/folder convention inside the testkit's driver layer (e.g. `driver-port/.../shop/api`, `driver-adapter/.../shop/ui`). This is the SUT-internal driver namespace, paired with `external/` (drivers for external systems). The name is part of ATDD doctrine and is **not** the repo name. Do not rename it.
- **`shop` (no slash, used in repo context)** — the repository name of the system under test. When `gh optivem atdd install` copies these prompts into a student repo, this gets substituted with the student's repo name. The `shop/` package convention stays unchanged.

The two uses are kept textually distinct (slash vs. no-slash) so the install-time substitution can target one without disturbing the other.
