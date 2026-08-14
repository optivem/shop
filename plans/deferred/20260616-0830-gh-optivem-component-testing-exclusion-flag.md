# gh-optivem generation flag to exclude the component/Pact layer (DEFERRED)

**Source plan:** `plans/20260616-0830-component-pact-layer-opt-in-canonical-projects.md` (Decisions → "No generation flag now").
**Status:** Deferred — build **only if** real demand appears. Lives in `gh-optivem`, not `shop`.

## Why deferred

The component/Pact layer is already isolated **off the default build** (Java `componentTest` source set; frontend `test:component`/`test:pact` scripts excluded from default `npm test`). So it is never *forced* — "present but dormant" already satisfies the goal of not pushing existing ATDD students onto component tests. A generation-time toggle only buys "not even present in the generated files," which is a weak need given the layer can simply be ignored or deleted.

The one residue a flag would remove: a generated frontend's `npm install` pulls the Pact/Vitest/RTL **devDependencies** even when unused (install size/time only — not runtime; Java is unaffected because `componentTestImplementation` resolves only when the opt-in task runs). If that residue becomes a real complaint, build the flag.

## End result (if built)

`gh optivem` generation accepts an **opt-out** flag set (default still *includes* the layer):

- **`--no-component-testing`** (≡ `--no-component-tests`) — omit the entire layer (component + Pact) from the generated project.
- **`--no-contract-tests`** — omit only the Pact files; keep the component tests.

**Dependency to encode:** the provider Pact test extends the component harness (`BackendPactVerificationTest extends BaseComponentTest`), so excluding component tests must cascade to excluding contract tests; the reverse is independent.

## Steps (if built)

- [ ] Step 1: **Config field** — add the component-testing inclusion flags to the gh-optivem config model (`internal/config`, `internal/kernel/projectconfig`) + CLI parsing, defaulting to *included*.
- [ ] Step 2: **Scaffolding conditional** — make `internal/scaffolding/**` skip the component/Pact files/source-set/scripts when the flag is set (cascade component → contract).
- [ ] Step 3: **Tests** — config + scaffolding unit tests for: default includes layer; `--no-component-testing` omits all; `--no-contract-tests` keeps component, drops Pact; cascade invariant holds.
- [ ] Step 4: **Docs** — update gh-optivem CONTRIBUTING + generated-project README to describe the flags.

## Notes

- This is purely additive to the generator; a `shop` already carrying the isolated layer needs no change.
- If the cross-language mirror (`plans/deferred/…-other-multitier-backends.md`) lands first, the flag should gate all three backends uniformly.
