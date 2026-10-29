# 2026-06-18 07:01 UTC — Place-Order test-levelling worked example (system vs component vs unit)

## TL;DR

**Why:** A reader (Vivek) asked, on the Optivem Substack chat
(<https://substack.com/chat/824051/post/29d6c44b-23d4-4aaa-99ef-1cfa3519dd4d>):
*"How much to cover in system / component acceptance tests keeping test
duplication in mind?"* — using a password-reset story with edge cases (link
expiry, password rules, single-use link). We want to answer it with a **real,
runnable place-order example** in this repo, so the paid article can embed
actual code samples instead of invented ones. **The article is blocked on this
plan**: its code blocks are placeholders until the example below exists and is
labelled for quoting.

**Article (the consumer of this plan):** `optivem/substack` →
`articles/deferred/PAID-ATDD-acceptance-tests-arent-duplicated.md` — it carries a
matching `BLOCKED ON:` marker pointing back at this plan file. Note: that
shop-anchored article is currently **deferred** (the active answer to Vivek is
the no-shop companion `articles/drafts/PAID-ATDD-which-tests-at-which-layer.md`),
so this plan is lower-priority until the deferred article is picked back up.

**End result:** For the `PlaceOrder` feature, every claim the article makes is
backed by a real, labelled test at the layer the article says it lives at —
the same rule visible at system / component / unit with a clear reason each
home was chosen — plus a one-page mapping doc the article links to.

## The gap (why this isn't already done)

What exists today, for `PlaceOrder`:

- **System acceptance** — strong. `system-test/java/.../acceptance/PlaceOrderPositiveTest.java`
  (full pricing matrix: base price, discount, tax, totals, combos) and
  `PlaceOrderNegativeTest.java` (the full validation-error-contract matrix:
  `fieldErrorMessage("quantity", …)` across `@Channel({UI, API})`), plus
  `PlaceOrderNegativeIsolatedTest.java` (`@Isolated`, controllable clock —
  year-end blackout, expired coupon).
- **Component** — exists **only in multitier Java**, split across
  `system/multitier/backend-java/src/componentTest/.../PlaceOrderPositiveComponentTest.java`
  (the pricing matrix — base price, promotion, coupon, tax, totals) and
  `.../PlaceOrderNegativeComponentTest.java` (validation + existence rules + the
  year-end blackout), in-process API, WireMock externals, Testcontainers DB. This
  is the deliberately opt-in layer. **Not** in monolith, backend-.NET, or
  backend-TS. (The former combined `PlaceOrderComponentTest` was removed
  2026-07-17; its unique scenarios were folded into the two files above.)
- **Unit** — **missing for pricing/validation.** `system/multitier/backend-java/src/test`
  holds only `BackendApplicationTests` (smoke) and `AbstractIntegrationTest`.
  There is **no quotable unit test** exhausting the pricing arithmetic or the
  validation rules — yet the article's whole "push the matrix down to unit"
  claim needs one to point at.

So the article cannot currently show the bottom of the pyramid, and the
system↔component overlap (the centrepiece — *same total, different risk*) is
real but unlabelled and undocumented.

## The teaching point the example must make

The article's thesis, which the code must demonstrate concretely:

1. **The overlap is not the duplication to kill.** `totalPriceShouldBeSubtotalPricePlusTaxAmount`
   (system) and `computesTotalsFromPricePromotionAndTax` (component) assert the
   *same arithmetic* but verify *different risks*: component = "does the backend
   compute it right?" (cheap, every commit); system = "is it reachable and
   correctly surfaced through the real deployed UI + API?" (can only live there).
2. **Each risk gets exactly one home; each scenario is authored once** in the
   Scenario DSL and *projected* across channels (`@Channel`) and isolation
   modes (`@Isolated`) — never hand-rewritten per layer.
3. **"Push it down" = push to the cheapest layer that still covers the risk** —
   not the cheapest layer that can run the assertion. Pure arithmetic → unit;
   computation wired to DB/externals → component; channel/integration surfacing
   → system.

**Spine of the worked example: order pricing logic.** Everything below is
built around the one rule family that already spans all three layers and is
easy to reason about — how an order's price is computed (base = unit × qty →
promotion → coupon discount → tax → total). Pricing is the thread the article
follows top to bottom, so the build-out concentrates the new component and unit
tests there.

- **A built-out unit layer for order pricing logic** in
  `system/multitier/backend-java` — pure JUnit tests (no Spring context, no DB)
  exhausting the pricing arithmetic matrix (base = unit × qty; subtotal after
  promotion; subtotal after coupon; tax amount; total; rounding / half-up
  boundaries) directly against the domain/service calculation. This is the
  "exhaust the matrix here" tier the article points to — and the place where the
  combinatorial rows live cheapest.
- **A built-out component layer for order pricing logic** — extend
  `PlaceOrderPositiveComponentTest` (in-process API, WireMock externals,
  Testcontainers DB) so the pricing matrix has a deliberate *component* home: the
  price × promotion × coupon × tax combinations that prove the backend
  *computes and persists* the totals correctly when wired to its real DB and
  stubbed neighbours, every commit, without a deployed UI. Today it has a few
  representative cases; build it into the clear cheaper twin of the system
  pricing matrix.
- **A quotable unit layer for `PlaceOrder` validation** — pure unit tests for the
  quantity/sku/country/coupon validation rules (the logic behind the
  `fieldErrorMessage` contracts), so the article can show the rule exhausted at
  unit while only the *contract* is asserted at system.
- **An explicit system↔component pricing twin, labelled**, so the article can
  quote both sides side by side and name the risk each covers. Centrepiece:
  **tax-inclusive total** (`totalPriceShouldBeSubtotalPricePlusTaxAmount`
  ↔ `computesTotalsFromPricePromotionAndTax`). Add a short
  `// LEVELLING: …` comment on each so the pairing is discoverable, not folklore.
- **One mapping doc** — `docs/atdd/test-levelling-place-order.md` — a single
  table: each `PlaceOrder` behaviour/edge case → its home layer → the risk that
  justifies that home (and why it is *not* also written elsewhere). The article
  links to this doc; future readers get the durable artefact.
- **Vivek's three edge cases mapped onto place-order analogues**, each with a
  real test to cite:
  - *reset-link expiry* → time-dependent rule → `cannotPlaceOrderWithExpiredCoupon`
    (`@Isolated`, controllable clock) at acceptance + the validity-window branch
    matrix at unit.
  - *password-rule violations* → pure validation → unit matrix + **one** surfaced
    field-error contract at system (`shouldRejectOrderWithInvalidQuantity`).
  - *single-use reset link* → state-transition rule → component
    (`cannotPlaceOrderWithCouponThatHasExceededUsageLimit` is today's system-level
    analogue; decide its correct home in Step 4).
- **Scope: multitier Java only.** Matches how the component layer is already
  scoped (opt-in, Java). No monolith / .NET / TS work; a follow-up may mirror it.

## ▶ Next executable step (resume here)

**Step 1** — audit + freeze the mapping. Read the four `PlaceOrder` test files
named in *The gap* and fill in `docs/atdd/test-levelling-place-order.md`'s table
(behaviour → layer → risk) for what exists **today**, marking each row
`✅ exists` / `🔨 to-build`. This table is the contract the rest of the steps
build to and the article quotes. No test code yet.

## Steps

- [ ] **Step 1 — Mapping doc (source of truth).** Create
  `docs/atdd/test-levelling-place-order.md`: one table, every `PlaceOrder`
  behaviour/edge case → home layer → risk justifying that home → status
  (`exists` / `to-build`) → the test method that proves it. Markdown only, no
  Pages (repo rule). The article links here.
- [ ] **Step 2 — Build the unit layer for order pricing logic.** Add pure unit
  tests (new `unit` package under `system/multitier/backend-java/src/test`)
  exhausting the pricing arithmetic against the domain/service calc — base =
  unit × qty, promotion, coupon discount, tax amount, total, and rounding /
  half-up boundary cases. No Spring, no DB. These are the "matrix lives here"
  samples the article quotes for the bottom of the pyramid.
- [ ] **Step 3 — Build out the component layer for order pricing logic.** Extend
  `system/multitier/backend-java/src/componentTest/.../PlaceOrderPositiveComponentTest.java`
  so the price × promotion × coupon × tax matrix has a deliberate component home
  (in-process API, WireMock externals, Testcontainers DB) — the cheaper twin of
  the system pricing matrix, proving compute-and-persist correctness every
  commit without a deployed UI.
- [ ] **Step 4 — Unit layer for validation.** Pure unit tests for the
  quantity/sku/country/coupon validation rules (the logic behind the
  `fieldErrorMessage` contracts), so the article can show the rule exhausted at
  unit while only the *contract* is asserted at system.
- [ ] **Step 5 — Label the system↔component pricing twin.** Pick the centrepiece
  pair (recommend tax-inclusive total), add `// LEVELLING:` comments to both
  naming the risk each owns, and decide + record the correct home for the
  coupon-usage-limit state rule (currently system; likely belongs at component).
- [ ] **Step 6 — Fill `to-build` gaps.** For any row the article needs that is
  still `to-build` after Steps 2–5 (e.g. an expiry-boundary unit matrix to pair
  with `cannotPlaceOrderWithExpiredCoupon`), add the missing test and flip the
  row to `exists`.
- [ ] **Step 7 — Compile + sample verify.** `./gradlew build` in
  `system/multitier/backend-java`; component tests are opt-in so confirm they
  still run under their task. (Ask before running system tests locally — memory.)
- [ ] **Step 8 — Hand article the citations.** Produce the final list of
  `file:method` references (with line anchors) the article embeds, and confirm
  every prose claim has a matching row in the mapping doc. Unblocks the article.

## Decisions

1. **Java-only**, mirroring the existing opt-in component layer. No
   cross-language fan-out in this plan.
2. **Add a real unit tier rather than weaken the claim.** The article asserts
   the matrix is exhausted at unit; the repo must actually show it. The unit
   tests are pure (no Spring/DB) so they stay fast and quotable.
3. **The mapping doc is the contract, authored first (Step 1).** Both the code
   work and the article's prose are checked against it, so they can't drift.
4. **Don't manufacture duplication to make a point.** Keep each scenario at one
   home; the system↔component overlap we showcase is the *legitimate* kind
   (same number, different risk), explicitly labelled as such.

## Open questions

- **Q1 — Coupon-usage-limit rule's correct home.** Today
  `cannotPlaceOrderWithCouponThatHasExceededUsageLimit` is a *system* test. Is
  its real risk the DB state transition (→ component, cheaper) or the
  user-facing rejection contract (→ keep one at system)? Resolve in Step 4 and
  record in the mapping doc — it is itself a good article example of the
  decision being made.
- **Q2 — Expiry-boundary coverage.** Do we add a unit-level validity-window
  matrix (just-expired / just-valid / boundary second) to pair with the
  `@Isolated` acceptance test, or is the single acceptance test enough for the
  article's purpose? Decide in Step 5.
