# 2026-11-07 16:38:37 UTC — Storefront use cases, business rules and acceptance criteria

## TL;DR

**Why:** The storefront redesign (`docs/design/shared/storefront-ux.md`, implementation plan `plans/20261107-1657-storefront-implementation.md`) describes screens, but not the behaviour behind them as use cases. Only UC-001 Place Order has a narrative, business rules and Gherkin acceptance criteria. Without those, the implementation steps (catalog, profile, tax by shipping country, checkout) have no agreed spec to drive ATDD.
**End result:** Every storefront use case has a narrative (main success scenario plus extensions), numbered business rules and tagged Gherkin acceptance criteria, in the same format as UC-001. All specs live under `docs/shared/specs/`, with the use-case diagram as their index. UC-001 is amended so tax uses the shipping country.

## Outcomes

What we get out of this:

- All specs live in one place, a folder per use case:
  ```
  docs/shared/specs/use-cases/
    README.md                     ← index: diagram + table (ID, name, actor), links to each folder
    UC-001-place-order/
      use-case-narrative.md                 ← narrative: actors, main success scenario, extensions
      business-rules.md           ← only when the use case has rules
      acceptance-criteria.md      ← front matter (id, title) + tagged Gherkin; what the spec gate reads
    UC-002-view-order/
      ...
  ```
  The existing `docs/use-cases/UC-001-place-order.md` is split into the UC-001 folder (with `git mv` for the narrative). `docs/business-rules/UC-001-place-order.md` moves in as `business-rules.md`, and `docs/design/shared/use-cases.md` becomes `README.md`. All references are updated.
- The spec gate reads the new layout. That needs gh-optivem to read `<specDir>/*/acceptance-criteria.md`, requested as C1b in the Place Order plan's Track C. The gate stays off until then, so nothing breaks in between.
- `README.md` shows every use case, existing and storefront, with Visitor, Customer and Admin actors (plus ERP and Clock). It's linked from the README, and each use case on it links to its UC file.
- A narrative for every use case on the diagram, using UC-001's format: front matter, actors, Main Success Scenario, Extensions. That includes the existing but undocumented ones (Cancel Order, View Order, Publish Coupon, Browse Coupons).
- Numbered business rules for each use case that has rules (catalog filtering and sorting, cart quantities, address validation, tax country, one order per cart line, admin visibility, cancel window, coupon validity).
- Gherkin acceptance criteria in each use case, tagged `@UC-NNN.M`, so later implementation steps can turn them into acceptance tests one scenario at a time.
- UC-001 is amended: the country comes from the shipping address, not a typed-in field. Its rules and scenarios are updated to match, in coordination with the in-flight UC-001 plan.
- The storefront implementation plan's steps each point to the use cases and scenarios they implement.
- Docs only: no code changes.

## ▶ Next executable step (resume here)

Steps 8 and 9 landed in unit U3 of `plans/20261107-1705-meta-storefront-specs-build.md` (UC-001 takes a shipping address, 4b = address/country; `storefront-ux.md` links the specs). Only the standing rule (Step 10) and the user's optional review of UC-002…012 remain; the SMETA orchestrator deletes this plan at its close-out (Step 8).

## Steps

- [x] Step 0: Requested gh-optivem support for the folder layout as C1b in `plans/20261106-1533-use-cases-match-substack-articles.md` Track C.
- [ ] Step 10: Commit and push only these docs files after each step, never the other agent's work.

## Use-case list (approved 2026-11-07; index in `docs/shared/specs/use-cases/README.md`)

- Existing:
  - UC-001 Place Order (Customer)
  - UC-002 View Order (Customer: my orders, order details)
  - UC-003 Cancel Order (Customer)
  - UC-004 Publish Coupon (Admin)
  - UC-005 Browse Coupons (Admin)
- Storefront:
  - UC-006 Browse Catalog (Visitor: list, category filter, brand filter, sort, empty and ERP-unavailable states)
  - UC-007 View Product (Visitor: product detail, unknown SKU, quantity stepper)
  - UC-008 Manage Cart (Visitor: add, change quantity, remove, cart survives sign-in, empty cart)
  - UC-009 Check Out (Customer: sign-in round trip, saved address or a different one, first order with an empty profile and "Save as default" ticked, coupon, one order per cart line through UC-001, partial failure)
  - UC-010 Manage Profile (Customer: name, shipping address, country; tax-country hint)
  - UC-011 View All Orders (Admin: every customer's orders, customer filter)
- Added 2026-11-07 (gap found in storefront-ux.md §7, the `Deliver Order` button on order details):
  - UC-012 Deliver Order (Admin; admin-only in every backend)

## Decisions (resolved 2026-11-07, user approved all recommendations)

- Numbering: existing features first (UC-002 to UC-005), then storefront (UC-006 to UC-011), then UC-012 Deliver Order. All twelve are storefront use cases on one diagram; "existing" only means already built.
- Each use case is a folder: `use-case-narrative.md`, `business-rules.md` (only when there are rules) and `acceptance-criteria.md` (front matter + Gherkin). The index is `use-cases/README.md`.
- Acceptance criteria granularity: one `@UC-NNN.M` scenario per business rule and per extension, as in UC-001.
- Gate status: every new `acceptance-criteria.md` gets `status: planned` in its front matter (next to `id`, `title`). The implementation plan flips it to `implemented` when that use case's acceptance tests land. UC-001 is `implemented`. gh-optivem will skip `planned` use cases (C1c). Decided 2026-11-07, see `plans/20261107-1705-meta-storefront-specs-build.md`.
- Checkout partial failure: the other lines are still placed; the confirmation page shows a per-line result.
- Review gates: the Step 2 list and IDs are approved as written. The UC-006 template review was waived when the user asked to work autonomously (2026-11-07); the user reviews all specs together instead.

## Findings from writing the specs (2026-11-07, for follow-up outside this plan)

Cross-language differences in current behaviour, found while writing UC-002 to UC-005 and UC-012. The specs state only the common behaviour:
- Cancelling a delivered order: `backend-clean-java` refuses it; every other backend allows it through the API (only already-cancelled is checked). The UIs hide Cancel once delivered.
- Coupon with valid-to before valid-from: Java monolith and backend-java throw an unmapped `IllegalArgumentException` (probably a 500); .NET and TypeScript don't check at all.
- Deliver Order button: the Java monolith's order-details page shows it to customers (the server then refuses); React shows it only to admins.
- React's coupon list has an extra Status column computed from browser time rather than the Clock.
- `backend-clean-java`'s BrowseOrderHistory has no owner/admin scoping and no customer field (UC-011 BR-5).
- UC-002 to UC-005 and UC-012 describe behaviour that is already built, but no test carries their `@UC-NNN.M` tags, so they stay `status: planned` until tests are tagged (added as a step to the implementation plan).
