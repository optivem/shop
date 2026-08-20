# 2026-08-18 16:59 CEST — `component-tests.yaml` for `backend-clean-java`

## TL;DR

**Why:** `system/multitier/backend-clean-java` is now declared as `kind: component`
(`gh-optivem-multitier-clean-java.yaml`, commit `3a904ab1`), so `gh optivem
compile` and `gh optivem component-test` both target it. But the directory has no
`component-tests.yaml`, and that file is where a component's suites live. So
`gh optivem component-test run` discovers the component and then runs **nothing**
— a silent green. Its real gate lives only in the commit-stage workflow, as
hand-written `./gradlew` steps that no local command reproduces.

**End result:** `backend-clean-java` carries a `component-tests.yaml` describing
the same pyramid its commit stage runs, so `gh optivem component-test run` gates
locally exactly as CI does, and `gh optivem compile` compiles the test source
sets up front. The workflow keeps its explicit Gradle steps (see Decision 2) but
its now-stale docstring stops claiming a config would over-activate the variant.

## Outcomes

- `gh optivem component-test run` from the repo root runs the clean variant's
  unit, integration, component and contract suites — the same set CI gates on —
  instead of finding zero suites and passing.
- `gh optivem compile` compiles the variant's test source sets (`compileTestJava
  compileComponentTestJava compileContractTestJava compileIntegrationTestJava`),
  so a test-compile break fails fast in the local sweep rather than in CI.
- The variant stops being the odd one out: `backend-java` has a
  `component-tests.yaml`, and after this so does `backend-clean-java`. A
  contributor moving between the two finds the same file in the same place.
- The `multitier-backend-clean-java-commit-stage.yml` docstring reflects reality.
  It currently states the variant avoids a `gh-optivem-*.yaml` because one would
  "pull the variant into the config matrix that compile-all.sh,
  validate-all-gh-optivem-config.sh and the system-test stages all iterate over —
  which is exactly the activation we are avoiding." That config now exists, and
  the fear did not materialise: `kind: component` is precisely the shape that
  gets compile + component-test coverage **without** system activation.

## Background — what exists today

**The commit stage is the whole gate**, and it calls Gradle directly. Its steps,
in order (`.github/workflows/multitier-backend-clean-java-commit-stage.yml`):

| Step | Command |
|---|---|
| Compile | `./gradlew compileJava compileTestJava compileComponentTestJava compileContractTestJava compileIntegrationTestJava` |
| Unit | `./gradlew test` |
| Component | `./gradlew componentTest --tests 'com.mycompany.myshop.backend.component.*'` |
| Integration | `./gradlew integrationTest` |
| Contract (stub side) | `./gradlew contractTest --tests '…contract.internal.*' --tests '…contract.external.*.*Stub*'` |
| Simulator image | `./gradlew externalSimulatorImage` |
| Contract (real side) | `./gradlew contractTest --tests '…contract.external.*.*RealParity*'` |
| Lint | `./gradlew checkstyleAll` |

**`backend-java/component-tests.yaml` is the reference shape** — `setupCommands`,
`compileCommands`, `testFilter`, a `suites:` list (`unit`, `integration`,
`component`, `provider-verification`, `external-contract`,
`external-contract-real`), and a `suiteGroups.all` that deliberately **excludes**
`external-contract-real` so it stays opt-in via explicit `--suite`. Commands there
are written as `.\gradlew.bat` (the runner is Windows-first locally).

The clean variant's contract split is the same two halves, so the same
suite decomposition applies almost verbatim. The one difference worth care: the
clean variant's stub-side step passes **two** `--tests` filters
(`contract.internal.*` and `contract.external.*.*Stub*`) in a single Gradle
invocation, where `backend-java` splits those into two suites
(`provider-verification` and `external-contract`). Splitting is better — it gives
`--suite provider-verification` real meaning and matches the sibling — but it
must be verified to select the same classes, not assumed.

**`checkstyleAll` is not a test suite.** `backend-java`'s file does not model
lint as a suite and this one should not either; lint stays a workflow step.

**Nothing is currently broken.** `gh optivem compile -c
gh-optivem-multitier-clean-java.yaml` already passes — a component with no
`component-tests.yaml` is skipped silently by design, since `compileCommands` is
an additive field. This plan turns a silent skip into real coverage.

## ▶ Next executable step (resume here)

**Step 0 first — it is a hard blocker.** The locally installed `gh optivem` binary predates the
`kind:`/`component:` schema, so every verification step in this plan errors out until it is
upgraded. Once it is:

Start at Step 1: author
`system/multitier/backend-clean-java/component-tests.yaml`, modelled on
`system/multitier/backend-java/component-tests.yaml`, transcribing the commit
stage's Gradle steps into `compileCommands` plus five suites (`unit`,
`integration`, `component`, `provider-verification`, `external-contract`) and
one opt-in suite (`external-contract-real`), with `suiteGroups.all` excluding
the real-mode suite exactly as the sibling does. Read both the sibling file and
the workflow before writing — the suite/test-filter mapping is the whole
substance of this plan, and getting a filter wrong produces a suite that passes
by selecting nothing.

## Steps

- [ ] Step 0 (blocker): Upgrade the locally installed `gh optivem` binary. The installed build
  predates the `kind:`/`component:` config schema introduced in `3a904ab1`, so it fails with
  `field kind not found in type projectconfig.Config` — `./compile-all.sh` reports
  `gh-optivem-multitier-clean-java.yaml FAILED` in 00:00 for this reason (not a compile failure).
  Every verification step below shells out to `gh optivem component-test`, so none of them can run
  until this is done.
- [ ] Step 1: Author `system/multitier/backend-clean-java/component-tests.yaml` — `setupCommands` (pre-warm Gradle), `compileCommands` (the five `compile*Java` tasks the commit stage lists), `testFilter`, and the six suites described above. Mirror `backend-java`'s file for field shape, command style (`.\gradlew.bat`), `requiresDocker` flags, and the `suiteGroups.all` exclusion of `external-contract-real`.
- [ ] Step 2: Verify each suite selects a non-empty test set — run `gh optivem component-test run --suite <id>` per suite and confirm each reports executed tests, not a vacuous pass. Pay particular attention to the `provider-verification` / `external-contract` split (Step 1 divides one commit-stage invocation into two suites) and confirm the two together select exactly what the single workflow step selects.
- [ ] Step 3: Set each suite's `sampleTest` to a real test name from that suite's output, and verify `gh optivem component-test run --suite <id> --sample` runs it. Do not copy `backend-java`'s sample names — the clean variant's classes differ.
- [ ] Step 4: Run `gh optivem component-test run -c gh-optivem-multitier-clean-java.yaml` (bare, no `--suite`) and confirm the `suiteGroups.all` set runs green and `external-contract-real` is absent from it.
- [ ] Step 5: Run `gh optivem compile -c gh-optivem-multitier-clean-java.yaml` and confirm the component-test phase now compiles the test source sets instead of being skipped.
- [ ] Step 6: Update the docstring of `.github/workflows/multitier-backend-clean-java-commit-stage.yml` — the "adding one would pull the variant into the config matrix … exactly the activation we are avoiding" paragraph is now false. State what is true: the variant is declared `kind: component`, which gives it compile and component-test coverage with no system activation, and the workflow keeps calling Gradle directly for the reason in Decision 2.
- [ ] Step 7: Commit (ask first, per the repo's commit gate).

## Decisions taken (resolved before execution)

1. **Model lint as a suite?** — **No.** `checkstyleAll` is not a test; `backend-java` does not model it and neither should this. It stays a commit-stage step.
2. **Switch the commit stage to `gh optivem component-test run`?** — **No, not in this plan.** That is a CI-behaviour change with its own blast radius (runner availability on the GH runner, exit-code and log-format differences, the `externalSimulatorImage` ordering the real-mode suite depends on). This plan's contract is "the local command gates what CI gates"; making CI *call* the local command is a separate, larger question. Note it in the Step 6 docstring rewrite as the deliberate current state, not an oversight.
3. **Include `external-contract-real` in `suiteGroups.all`?** — **No.** Match `backend-java` exactly: it stays opt-in via explicit `--suite`, because it needs `externalSimulatorImage` built first and is slow. Consistency between the two variants matters more here than any independent judgement about the suite.
4. **Split the commit stage's single stub-side contract invocation into two suites?** — **Yes** (`provider-verification` + `external-contract`), matching `backend-java`. The two counterparty situations are genuinely different — one is a real pact the frontend verifies, the other is stub-vs-real parity for systems that will never run our verification — and naming them separately is the point. Step 2 verifies the split is faithful.

## Verification (operator, not agent steps)

- Confirm `./compile-all.sh` still reports seven PASSED rows after this lands.
- Confirm `./validate-all-gh-optivem-config.sh` still passes — it globs
  `gh-optivem-*.yaml` and so already picks up the new component config, running
  `config validate` + `config preflight` against it. Both were verified passing
  when the config landed; this is a regression check, not new ground.

## Related

- `plans/20260818-1326-compile-all-misses-backend-clean-java.md` — the coverage
  check that makes a silently-unclaimed project directory fail loudly. Same
  family of defect (a gate that reports green over work it never did), now
  unblocked by the same `kind: component` change. Independent of this plan; they
  can land in either order.
- `gh-optivem` commit `f3f003d0` — the `kind:` discriminator this rests on.
