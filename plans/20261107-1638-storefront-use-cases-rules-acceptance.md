# 2026-11-07 16:38:37 UTC — Storefront use cases, business rules and acceptance criteria

## TL;DR

**Why:** The storefront redesign (`docs/design/shared/storefront-ux.md`, plan `plans/20261107-1112-realistic-eshop-storefront-ux.md`) describes screens, but not the behaviour behind them as use cases. Only UC-001 Place Order has a narrative, business rules and Gherkin acceptance criteria. Without those, the implementation steps (catalog, profile, tax by shipping country, checkout) have no agreed spec to drive ATDD.
**End result:** Every storefront use case has a narrative (main success scenario plus extensions), numbered business rules and tagged Gherkin acceptance criteria, in the same format as UC-001. All specs live under `docs/shared/specs/`, with the use-case diagram as their index. UC-001 is amended so tax uses the shipping country.

## Outcomes

What we get out of this:

- All specs live in one place, a folder per use case:
  ```
  docs/shared/specs/use-cases/
    README.md                     ← index: diagram + table (ID, name, actor), links to each folder
    UC-001-place-order/
      use-case.md                 ← narrative: actors, main success scenario, extensions
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
- The storefront UX plan's Steps 2–8 each point to the use cases and scenarios they implement.
- Docs only: no code changes.

## ▶ Next executable step (resume here)

Step 1: move the specs to the folder-per-use-case layout.
- `git mv docs/use-cases/UC-001-place-order.md docs/shared/specs/use-cases/UC-001-place-order/use-case.md`, then move its front matter and the `## Acceptance criteria` Gherkin section into `acceptance-criteria.md` in the same folder.
- `git mv docs/business-rules/UC-001-place-order.md …/UC-001-place-order/business-rules.md`, and fix the BR link in `use-case.md`.
- `git mv docs/design/shared/use-cases.md docs/shared/specs/use-cases/README.md`.
- Update the references:
  - `spec.yaml` (`specDir: docs/shared/specs/use-cases`). Until gh-optivem C1b ships, the gate (already disabled) can't read it; add a comment saying so.
  - the disabled spec-gate comment in `.github/workflows/_acceptance-stage-docker-compose.yml` (~line 426)
  - the comment in `system-test/typescript/tests/commons/criteria.ts`
  - the paths in `plans/20261106-1533-use-cases-match-substack-articles.md`
- Coordinate first: that plan's agent owns UC-001 and the spec gate (track D will run against `spec.yaml`). Check `git status` and `git log origin/main` for in-flight work on those files.
- Compile isn't affected (docs, a YAML path and comments only). Commit only these files, then push.

That unblocks Step 2 (the full diagram).

## Steps

- [x] Step 0: Requested gh-optivem support for the folder layout as C1b in `plans/20261106-1533-use-cases-match-substack-articles.md` Track C.
- [ ] Step 1: Move the specs to the folder-per-use-case layout and update all references (see Next executable step).
- [ ] Step 2: Index first. Rewrite `docs/shared/specs/use-cases/README.md` as the index of every use case: a diagram plus a table. Actors: Visitor, Customer (a signed-in Visitor), Admin (renamed from "MyShop Owner"), ERP and Clock. Each node links to its UC file, and the README links to the diagram. User review gate on the list and IDs:
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
- [ ] Step 3: Write UC-006 Browse Catalog as the template-setting example (narrative, rules, Gherkin). Content comes from storefront-ux.md §4 and §6. User review gate.
- [ ] Step 4: Write the existing use cases UC-002 to UC-005. Write them from current behaviour: read the backends and the existing system tests so the narrative matches what the code does today.
- [ ] Step 5: Write UC-007 and UC-008 (product and cart).
- [ ] Step 6: Write UC-009 and UC-010 (checkout and profile). This includes the tax-country rule: tax rate = rate of the shipping address's country (US 0%, DE 19%, GB 20%).
- [ ] Step 7: Write UC-011 (admin all orders).
- [ ] Step 8: Amend UC-001. Country comes from the shipping address. Replace "4b country is missing" with an address-country extension, and update BR-3 and the affected `@UC-001.*` scenarios. Coordinate with `plans/20261106-1533-use-cases-match-substack-articles.md` first, since that plan's agent owns UC-001.
- [ ] Step 9: Link the specs from storefront-ux.md. Cross-reference the storefront UX plan: each of its Steps 2–8 lists the `@UC-NNN.M` scenarios it must make pass.
- [ ] Step 10: Commit and push only these docs files after each step, never the other agent's work.

## Open questions

- Numbering: existing features first (UC-002 to UC-005), then the storefront ones (UC-006 to UC-011). Or should the storefront ones come first? Settled at the Step 2 gate.
- Each use case is a folder: `use-case.md`, `business-rules.md` (only when there are rules) and `acceptance-criteria.md` (front matter + Gherkin). The index is `use-cases/README.md`, because GitHub shows it when someone opens the folder. (Decided 2026-11-07.)
- Acceptance criteria granularity: one scenario per business rule and extension, as in UC-001 (my default), or a lighter set covering the main paths?
- Checkout partial failure: if one cart line fails (for example, the coupon hits its limit on the second order), do the other lines still get placed? My default is yes, with a per-line result on the confirmation page.
