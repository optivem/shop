# 2026-11-06 15:33:50 UTC — Bring the Place Order use case in line with the Substack articles

> 🤖 **Picked up by agent** — `531392d34340` at `2026-11-06T20:23:15Z`

## TL;DR

**Why:** Two paid Substack articles use Place Order as the reader's worked example: `articles/published/2026-11-06-PAID-ATDD-acceptance-test-coverage-with-ai.md` and `articles/drafts/next/2026-11-06-PAID-ATDD-use-cases-stay-user-stories-go.md` in the `optivem/substack` repo. They show a full use case narrative and Gherkin acceptance criteria. The shop has an older, shorter narrative and one-line bullet criteria. A reader who opens the shop after reading either article finds something different from what the article showed.
**End result:** The shop's Place Order narrative and acceptance criteria match what the articles show. Every acceptance criterion has a tagged test. The spec gate is commented out until gh-optivem can read Gherkin criteria, and a follow-up plan brings it back.

## Outcomes

- `docs/use-cases/UC-001-place-order.md` holds the narrative exactly as the articles show it, in place of the two-line `## Main scenario`: primary actor Customer, secondary actors ERP and Clock, a 9-step main success scenario, and extensions 2a, 3a, 4a, 4b, 5a, 5b and 6a. The old `docs/design/shared/use-case-narrative.md` is gone, or it points to the new file.
- The narrative says ERP everywhere, never SAP.
- The acceptance criteria are Gherkin scenarios tagged `@UC-001.N`, as the articles show them. Each criterion that comes from an extension also carries that extension's tag: 2a → UC-001.12, 3a → UC-001.13, 4a → UC-001.8, 4b → UC-001.10, 5a → UC-001.14, 5b → UC-001.9, 6a → UC-001.11.
- UC-001.14 (ERP unavailable) has a tagged Java acceptance test.
- The `spec-gate` step is commented out, with a pointer to the follow-up plan `plans/move-to-gh-optivem/20261106-2030-spec-gate-read-gherkin-criteria.md`. `spec.yaml` stays. (Done.)

## Decisions (resolved 2026-11-06 with the user)

1. **Criteria format:** Gherkin scenarios, as the articles show them. gh-optivem's spec parser (`internal/spec/usecase.go`, `criterionPattern`) only reads bullet lines. A probe showed `gh optivem spec validate` passes with zero criteria found, so the gate would turn into a silent no-op. **Resolution:** comment out the `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` and keep `spec.yaml`. A follow-up plan in `plans/move-to-gh-optivem/` teaches the parser Gherkin and turns the step back on. Shop doesn't change gh-optivem directly.
2. **Deleting criteria once their tests pass:** keep the criteria in the shop for now. Tell the substack side about the conflict with the "delete" paragraph.
3. **.NET/TypeScript tags:** the criteria are already shared by all three languages (`docs/use-cases/`). Only the tests and the gate's `results` input are Java-only, and `results` already accepts a list. Tagging the .NET and TypeScript tests goes in its own plan under `plans/backlog/p3-test-parity/`, not here.

## ▶ Next executable step (resume here)

Step 1: write the narrative and the business rules file (see below). Source: the `# Use Case: Place Order` block under "Step 1.2" in `articles/published/2026-11-06-PAID-ATDD-acceptance-test-coverage-with-ai.md` (read it with `gh api repos/optivem/substack/contents/<path> -H 'Accept: application/vnd.github.raw'`).

## Steps

- [ ] Step 1: Write the narrative into `docs/use-cases/UC-001-place-order.md`, replacing the two-line `## Main scenario`. Copy the narrative block verbatim from the coverage article (the `# Use Case: Place Order` block under "Step 1.2. Generate the use case narrative for the core use case"). Also add `docs/business-rules/UC-001-place-order.md` with BR-1 to BR-4 from the same step. Use the version without Stakeholders, Preconditions, Trigger and guarantees: both articles dropped those sections on 2026-11-06.
- [ ] Step 2: Remove `docs/design/shared/use-case-narrative.md`, or replace its Place Order part with a link to the new file. Keep the template section only if something still uses it (grep the repo first). Update any links to the old path.
- [ ] Step 3: Rewrite the acceptance criteria in `docs/use-cases/UC-001-place-order.md` as Gherkin scenarios in the articles' format. Keep every ID, UC-001.1 to UC-001.14, unchanged, because the Java tests are tagged with them. Add the narrative tags (see Outcomes).
- [ ] Step 5: Add a Java acceptance test tagged `@Tag("UC-001.14")` for "ERP unavailable": the customer is told product information can't be retrieved right now. It goes in `PlaceOrderNegativeTest` or `PlaceOrderNegativeIsolatedTest`, whichever already stubs the ERP. Check that no test anywhere is tagged `UC-001.14` before adding one.
- [ ] Step 7: Tell the substack side which SHOP WORK notes are now done, so they can be removed from both articles: the narrative note and the Gherkin note. Also flag that the "delete criteria once their tests pass" paragraph conflicts with the shop keeping its criteria.

## Out of scope

- Gift wrapping, the story-to-criterion mapping, and the illustrative legacy examples (Track Shipment, the wrong draft of 6a, the year-end check). The articles flag these as illustrative SHOP WORK, not as things the shop must have.
- Use cases other than Place Order. The shop has only UC-001.
