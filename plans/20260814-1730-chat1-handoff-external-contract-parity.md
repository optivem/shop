# chat1 handoff — external-system contract parity

**Author:** `chat1` (Claude Code session, 2026-08-14)
**Purpose:** close-out record so this chat can be ended. Everything `chat1` decided, landed, broke,
and left open is captured here. A later session should be able to pick up from this file alone.

**Status:** Handoff — **one urgent item below, then open questions to decide.**

---

## 🔴 Do this first: `origin/main` is broken

`external-systems/simulators/mock-server.js:137` on `origin/main` reads:

```js
time: '2029-09-09T09:09:09.000Z'   // WRONG — on origin/main
```

It should read:

```js
time: '2024-01-15T10:30:00.000Z'   // correct — sitting uncommitted in the working tree
```

**How it happened.** `chat1` temporarily mutated that timestamp to prove the new
`ClockRealParityContractTest` was not passing vacuously. The mutation worked exactly as intended (the
test went red, then green again on revert), but a concurrent commit — `df302fc0` — ran *during* that
window, swept the mutated file in, and pushed. The revert is in the working tree, uncommitted, because
the user elected to commit it themselves.

**Consequence.** `BaseClockTimeParityContractTest.PINNED_TIME` expects `2024-01-15T10:30:00.000Z`, so
the new `external-contract-real` commit-stage step fails on `main` until this lands.

**Blast radius.** Small. Grepping `system/` and `system-test/` for the timestamp turns up only that one
Java constant; nothing else asserts on it.

**Verify after committing:**

```bash
cd system/multitier/backend-java
./gradlew externalSimulatorImage contractTest --tests '*RealParityContractTest'   # expect 5 passed
```

**Lesson worth keeping:** mutation-test against a scratch copy, never the tracked simulator file, while
other agents may be committing.

---

## What `chat1` landed

Two commits, both already on `origin/main`:

- `f01e811e` — contract tests moved into the `contractTest` source set; Consumability/Parity
  vocabulary; `Abstract*` → `Base*` test base classes.
- `df302fc0` — external contract tests reorganised by counterparty; **clock and tax parity twins
  added**; `external-contract-real` suite wired into commit stage. (Also the commit that captured the
  bad timestamp above.)

**Coverage moved from one parity pair to three.** ERP, tax and clock each now have a `Stub`/`Real`
pair. One Testcontainers-supplied simulator container serves all three
(`contract/external/ExternalSystemSimulator`), on a random port, fresh per run.

**Verified before `df302fc0`:** 26 `contractTest` tests pass (5 real-mode, 8 stub-side, 13 Pact);
`checkstyleAll` clean; and a deliberate mutation of the simulator proved the real twins actually detect
drift rather than passing vacuously.

---

## Decisions `chat1` made — do not relitigate

1. **Parity tests belong in commit stage, not acceptance.** Not a cost/benefit call. A parity test
   establishes that the stub is trustworthy enough for the `component` suite to assert against; a test
   guarding a suite's validity must not run later than the suite it guards, or a green commit stage can
   ship on a stub already known-wrong downstream. This retires the acceptance-stage option outright.
   *(User's argument, recorded in `20260812-1600`.)*
2. **The suite is blocking, not advisory.** An allowed-to-fail parity check reintroduces the exact hole
   it exists to close.
3. **Testcontainers, not docker-compose.** Keeps commit stage's "containers come from Testcontainers"
   convention intact, and buys a random port (no fixed `9111`, no collision with the prerelease
   pipeline's pre-`system start` ordering) plus per-run freshness.
4. **The image is built by a Gradle `Exec` task (`externalSimulatorImage`), not
   `ImageFromDockerfile`.** Verified constraint: `ImageFromDockerfile` drives docker-java's *classic*
   builder, which rejects the simulator's `RUN --mount=type=cache` with "the --mount option requires
   BuildKit". Only the image *build* moves out; Testcontainers still owns the container lifecycle.
   The task is deliberately **not** a `dependsOn` of `contractTest`, so other contract suites don't pay
   for it.
5. **`external-contract-real` stays out of `suiteGroups.all`** — opt-in via explicit `--suite`.
6. **Clock parity is unarranged by necessity.** `/clock/api/time` is a hardcoded handler, not a mutable
   resource, so parity pins the announced value from both sides instead of provisioning a fixture.

---

## Connected plans

### Executed by `chat1` — need a disposition decision

| Plan | State |
|---|---|
| `20260812-1600-erp-real-contract-ci-wiring.md` | **Fully implemented.** Its four open questions are resolved in-file. |
| `20260814-1610-tax-real-parity-contract-test.md` | **Fully implemented** (tax parity built, clock decided explicitly — see decision 6). |

Under the plan-processing rule both should now be deleted. `chat1` did **not** delete them, because
their resolutions hold reasoning only partly mirrored into `docs/atdd/test-taxonomy.md` — notably the
BuildKit constraint (decision 4), which is captured in code comments (`build.gradle`,
`ExternalSystemSimulator`) but not in any doc. **See open question 1.**

### Open, untouched by `chat1`

| Plan | Connection |
|---|---|
| `20260814-1530-legacy-twin-stub-fidelity-gap.md` | **Directly downstream.** Legacy twins hand-write stub JSON that nothing guards; after a real-system field rename they can keep passing while teaching a wire format that no longer exists (`@JsonIgnoreProperties(ignoreUnknown = true)` hides additive drift). Recommends **option A** (replace the unbacked "byte-identical" prose claim with an honest note), with option C (delete under-performing legacy twins) split into its own discussion. **Its open question 1 is now answered** — tax and clock are no longer unguarded, so the "unguarded on both sides" argument it raises has narrowed to the legacy side only. |
| `20260717-1015-component-stub-contract-beyond.md` | Same suite. Its item 4 is the standing ruling that promotion and error-injection (`500`/`503`) real-mode twins are not buildable against this simulator — the reason parity stays scoped to the happy-path read on all three externals. |

### Adjacent, not touched

- `20260717-1020-orderhistory-systemtest-dsl-parity.md` — system-test DSL, decided and deferred.
- `20260722-1216-string-only-money-surface.md` — money/rate surface in the Java test DSLs. Note the new
  parity tests take rates as `String` (`ARRANGED_RATE = "0.11"`), consistent with that plan's direction.
- `20260722-1221-meta-test-dsl-coordination.md` — the earlier coordination meta-plan for test-DSL work.
  This file does **not** supersede it; different scope.

---

## Open questions / topics

Each has a `chat1` recommendation. None is blocking except the urgent item above.

1. **Disposition of the two executed plans.** Delete per the plan-processing rule, or keep as decision
   records?
   *Recommendation:* mirror the BuildKit constraint (decision 4) into `docs/atdd/test-taxonomy.md`,
   then delete both plans. Keeps the rule intact without losing the one fact that lives nowhere but a
   code comment.

2. **`contract/legacy/external/erp/ErpStubParityContractTest.java` — keep or drop?**
   In-flight work whose own Javadoc argues against keeping it ("Read this one next to
   `integration/legacy/ErpGatewayIntegrationTest` before keeping it"). It currently makes
   `external-contract` run `ErpStubParityContractTest` **twice**.
   *Recommendation:* fold into the option-C discussion in `20260814-1530` rather than deciding it
   standalone — it is the same question about how many "before" twins the teaching material needs.

3. **`.NET` and TypeScript backends have no `contract/external/` suite at all.**
   Neither the parity guard nor `external-contract-real` exists in two of three languages, and the gap
   widens every time backend-java's suite grows. Flagged as open question 3 in `20260814-1610` and
   called out by the repo's own "check all languages" rule.
   *Recommendation:* the largest remaining gap. Worth its own plan before backend-java grows further.

4. **Legacy-twin fidelity** — decide `20260814-1530` (option A recommended there).

5. **Uncommitted in-flight work not owned by `chat1`:** modified
   `integration/latest/{Clock,Tax}GatewayIntegrationTest.java` and untracked
   `integration/legacy/{Clock,Tax}GatewayIntegrationTest.java`. `chat1` did not touch, compile, or
   review these.

---

## Reference — commands

```bash
# Real-mode parity only (builds the simulator image first)
cd system/multitier/backend-java
./gradlew externalSimulatorImage contractTest --tests '*RealParityContractTest'

# Whole contract layer
./gradlew externalSimulatorImage contractTest

# Lint every source set, including the opt-in test layers
./gradlew checkstyleAll

# Run against an already-running simulator instead of Testcontainers
EXTERNAL_SIMULATOR_BASE_URL=http://localhost:9111 ./gradlew contractTest --tests '*RealParityContractTest'
```
