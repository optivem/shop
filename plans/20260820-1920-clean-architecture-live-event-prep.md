# 2026-08-20 19:20:18 UTC — Clean Architecture live-event demo prep (backend-clean-java)

## TL;DR

**Why:** `system/multitier/backend-clean-java` is the code demo for the paid live event *Clean
Architecture: Stop doing it wrong* (2026-08-26, 90 min). An audit on 2026-08-20 found the code
covers all three advertised failure modes, but only theme 2 has a written artefact behind it —
themes 1 and 3 exist as code and README bullets with no before/after narrative. One code gap
(theme 3's wire types being publicly reachable) was fixed the same day and still needs its
Docker-backed suite run before it can be committed.

**End result:** All three themes are demo-ready on the same footing: each has a named before/after
file pair, a stated punchline, and a written artefact under `docs/atdd/code/`. The theme-3 fix is
verified and committed. Nothing in `backend-clean-java/README.md` that might go on screen states a
false measurement.

## Outcomes

What we get out of this — the goals and deliverables:

- **The theme-3 visibility fix is verified and committed.** `componentTest` (62 tests, needs Docker)
  green against the changed `BaseComponentTest` wiring, so the working tree stops holding
  unverified changes six days before the event.
- **Theme 1 has a written artefact** naming the before/after file pair
  (`backend-java/core/entities/Order.java` vs `backend-clean-java/domain/entities/Order.java` +
  `OrderJpaEntity` + `OrderMapper`) and the ArchUnit rules that hold it, at the level of detail
  `theme2-measurements.md` reaches for theme 2.
- **Theme 3 has a written artefact** naming the before/after pair and the exact punchline lines —
  `OrderService.java:64-65` (`promotion.isPromotionActive() ? promotion.getDiscount() : ONE`) vs
  `PlaceOrder`'s `erpGateway.getPromotionDetails().factor()` — plus the package-private boundary and
  the stub-vs-real parity contract tests as the "how do you know the supplier changed?" answer.
- **`backend-clean-java/README.md` states nothing false.** The "Shared vs duplicated — the
  measurement" section is either re-measured or cut back to the claim that still holds.
- **A demo running order exists** — which files open in which sequence, per theme, so the 90 minutes
  are rehearsable rather than improvised.

## ▶ Next executable step (resume here)

**Run `./gradlew componentTest` in `system/multitier/backend-clean-java` and commit the theme-3
visibility fix if it is green.**

The working tree currently holds 12 modified files (verified via `./gradlew build
componentTestClasses integrationTestClasses contractTestClasses benchmarkClasses checkstyleAll` —
BUILD SUCCESSFUL, 97/97 unit tests, and `integrationTest --tests '*GatewayIntegrationTest'` — 27/27):

- `src/main/.../infrastructure/external/` — `HttpErpGateway` and `HttpTaxGateway` fetch methods made
  `private`; `ProductDetailsResponse`, `GetPromotionResponse`, `TaxDetailsResponse`, `GetTimeResponse`
  made package-private classes.
- `src/testSupport/.../driver/adapter/sut/` — `SutErpReader` / `SutTaxReader` now depend on the
  domain ports and return `Product` / `TaxRate`, matching `SutClockReader`.
- `src/testSupport/.../then/steps/` — `ThenProductImpl` / `ThenCountryImpl` assert on domain values.
- `src/componentTest/.../BaseComponentTest.java` — autowires `ErpGateway` / `TaxGateway`.
- `README.md` — the "Gateways return domain types" bullet restated.

`componentTest` is the only gate not yet run; it needs Docker (Postgres via Testcontainers) and it
is the suite that exercises the changed `BaseComponentTest` wiring. **Ask before starting it** — the
user does not want local Docker-backed suites self-initiated. If green, ask to commit, then proceed
to Step 3.

## Steps

- [ ] Step 1: Ask, then run `./gradlew componentTest` in `system/multitier/backend-clean-java`.
      Expect 62 passing. If anything fails, fix before going further — nothing below is worth doing
      on top of a broken demo.
- [ ] Step 2: Ask to commit the theme-3 visibility fix (12 files above).
- [ ] Step 3: Write the theme-1 artefact under `docs/atdd/code/`. Contents: the before/after file
      pair, what the CRUD `Order` is (`@Entity @Data @GeneratedValue`, zero behaviour) versus what
      the clean one is (private constructor, `place`/`restore`/`deliver`/`cancel`), where the
      surrogate key went (adapter only — see the `surrogate-key-stops-at-persistence` decision), and
      which ArchUnit rules make it non-negotiable.
- [ ] Step 4: Write the theme-3 artefact under `docs/atdd/code/`. Contents: the wire DTO's journey
      (`core/dtos/external/` → `infrastructure/external/`, public → package-private), the two
      punchline lines, the `GatewayException` family mapping to one 502, `ignoreUnknown = true`, and
      the stub-vs-real parity contract tests.
- [ ] Step 5: Resolve the README's "Shared vs duplicated — the measurement" section. It claims the
      `testSupport`/`componentTest` trees are identical file-for-file with 31 files differing; actual
      counts on 2026-08-20 are **106 differing and 5 only in the clean variant** — stale before this
      session's change, mostly from the javadoc-to-inline-notes commits.
- [ ] Step 6: Write the demo running order — per theme, which files open in what sequence, which
      commands run live, which numbers get quoted from `theme2-measurements.md`.

## Open questions

- **Naming for the theme 1 and 3 docs.** `theme2-measurements.md` is named for its numbers; themes 1
  and 3 are structural arguments with no measurements. Proposal: `theme1-orm-as-domain-model.md` and
  `theme3-external-boundary.md`, with theme 2 left as-is. Alternative: rename all three to a common
  `themeN-<topic>.md` shape.
- **Do themes 1 and 3 want measurements at all?** Theme 2's credibility comes from reproducible
  before/after numbers. Recommendation: **no** — theme 1 and 3 are correctness/coupling arguments, and
  inventing a metric for them would be weaker than the file diff. Worth confirming, because it sets
  how much work Steps 3–4 are.
- **README fork-measurement: re-measure or cut?** Recommendation: **cut** to the claim that survives
  ("zero assertion changes, total structural divergence") and drop the precise file counts, since
  they go stale every time either variant is touched. Re-measuring buys a number with a short
  shelf life.
- **Is the demo running order (Step 6) in scope here, or a separate artefact outside this repo?** It
  is presentation material rather than code documentation, so it may belong with the event assets
  rather than in `docs/atdd/code/`.
- **Does anything need to change in the sibling variants?** The theme-3 fix narrowed visibility in
  `backend-clean-java` only. `backend-java` keeps its public wire DTOs deliberately — that is the
  before-picture — but `backend-dotnet` / `backend-typescript` were not looked at. Probably out of
  scope for the event; worth a decision so it is not an accident.
