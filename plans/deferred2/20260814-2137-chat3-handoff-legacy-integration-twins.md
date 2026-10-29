# chat3 handoff — legacy narrow-integration twins

**Author:** `chat3` (Claude Code session, 2026-08-14)
**Purpose:** close-out record so this chat can be ended. What `chat3` landed and what it left open.
A later session should be able to pick up from this file alone.

**Status:** Handoff — **one urgent item (inherited, not caused here), then three small decisions.**

---

## 🔴 Do this first: `origin/main` is still broken

**This is `chat1`'s item, not `chat3`'s — see
`20260814-1730-chat1-handoff-external-contract-parity.md` for the full account.** It is repeated here
only because it was still unfixed when `chat3` ended, and because `chat3` is the session that
committed the bad value.

`external-systems/simulators/mock-server.js:137` on `origin/main` reads `2029-09-09T09:09:09.000Z`;
it should read `2024-01-15T10:30:00.000Z`. The correct value is sitting **uncommitted in the working
tree** and needs committing.

**How `chat3` is implicated.** `chat1` had temporarily mutated that timestamp to prove
`ClockRealParityContractTest` was not passing vacuously. `chat3`'s commit `df302fc0` ran a
whole-repo `--all` sweep during that window and pushed the mutated file. `chat3` then saw the
working-tree revert, could not account for it, and deliberately held it back from `b1b21445` —
correct instinct, wrong diagnosis: it is the fix, not a stray revert.

**Lesson for this repo, beyond `chat1`'s:** a whole-dirty-set `--all` commit is unsafe while another
session may be mid-experiment. Prefer `--paths` naming your own files when concurrent sessions are
plausible, and read `plans/*handoff*.md` before sweeping.

---

## What `chat3` landed

Two commits, both pushed to `main`.

**`df302fc0`** — reorganize external contract tests by counterparty; add Clock/Tax parity twins.
This was pre-existing in-flight work from earlier sessions that `chat3` committed as one sweep. It
is also the commit that swept in the bad timestamp above.

**`b1b21445`** — the actual `chat3` work: legacy narrow-integration twins.

Every gateway at the narrow-integration layer now has a `latest`/`legacy` pair:

```
integration/latest/    Clock, Erp, Tax   (+ base/)
integration/legacy/    Clock, Erp, Tax
```

New: `integration/legacy/ClockGatewayIntegrationTest`, `integration/legacy/TaxGatewayIntegrationTest`.
Each mirrors its `latest/` twin scenario-for-scenario against raw, inlined WireMock, repeating the
server lifecycle and reflective URL injection verbatim from the Erp twin. The triplicated harness is
deliberate — it is what `BaseGatewayIntegrationTest` deletes.

The Clock twin carries the sharper contrast: legacy can only set `external.system-mode` reflectively
from a bare `"real"` / `"bogus"` string literal, while `latest` passes a typed `ExternalSystemMode`
and keeps a raw string only for the unknown-mode branch, which typing would make unreachable.

**Verify:**

```bash
cd system/multitier/backend-java
./gradlew integrationTest --tests '*GatewayIntegrationTest'   # expect 26 passed
```

### Decision reversed mid-session — worth knowing

`df302fc0` added javadoc to `latest/{Clock,Tax}GatewayIntegrationTest` arguing these legacy twins
**should not exist** (one twin per distinct contrast; the Erp twin already carries it). The user
rejected that reasoning and asked for symmetry instead. `b1b21445` rewrites that javadoc.

The one carve-out that survived both rounds: **`contract/external/` still has no `legacy/` twin, and
that is not a subset choice.** A "before" there would be genuinely circular — a JSON literal asserted
against a parse three lines below it. That argument is in `docs/atdd/test-taxonomy.md:97-108` and
should not be flattened into the weaker "representative subset" rule.

---

## Open questions

### OQ1 — Stale javadoc in `legacy/ErpGatewayIntegrationTest.java:24`

It claims the 500/503 error-injection cases "stay raw in both twins." That contradicts
`latest/ErpGatewayIntegrationTest.java:19` ("Every stub — including the 500/503 error-injection cases
— is programmed through the DSL") and the code, which calls `erp().failsForProduct()`.

**Recommendation:** fix the legacy line; `latest` is the accurate one. One-line edit, no test impact.
Offered twice in `chat3`, never approved.

### OQ2 — `20260814-1530-legacy-twin-stub-fidelity-gap.md` scope is now stale

That plan enumerates the files that hand-write stub JSON outside the DSL choke point, and lists only
`integrationTest/.../integration/legacy/ErpGatewayIntegrationTest` for this layer. `b1b21445` added
two more hand-written-JSON twins at the same layer:

- `integration/legacy/ClockGatewayIntegrationTest` — inline `{"time":"2026-03-10T12:00:00Z"}`
- `integration/legacy/TaxGatewayIntegrationTest` — inline `{"id":"US","countryName":"US","taxRate":0.10}`

Both are byte-matched to their stub drivers today; nothing enforces that they stay matched. The Tax
one is the more exposed of the two, since `TaxDetailsResponse` has **no**
`@JsonIgnoreProperties(ignoreUnknown = true)` — so additive drift breaks it loudly, which is
arguably better than the Erp case that plan describes.

**Recommendation:** update that plan's Scope section to list all three, before deciding between its
options A–C. Cheap, and it changes the cost estimate for its options.

### OQ3 — Centralize the legacy-subset rule in `docs/atdd/test-taxonomy.md`?

Raised in `chat3` when the subset rule (one twin per distinct contrast) was still the operating
answer. **Recommendation: drop it.** The rule no longer describes this layer now that every gateway
has a twin, and `contract/external/` already has its own stronger, well-stated argument in that doc.
Recorded only so it is not rediscovered as an open thread.

---

## Not open

- `compile-all` / integration suite: green (26 passed at `b1b21445`).
- No plan items were consumed by `chat3`; nothing to delete from `plans/`.
