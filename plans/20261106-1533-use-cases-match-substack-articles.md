# 2026-11-06 15:33:50 UTC — Bring the Place Order use case in line with the Substack articles

## TL;DR

**Why:** Two paid Substack drafts show Place Order as the reader's worked example: `substack/articles/published/2026-11-06-PAID-ATDD-acceptance-test-coverage-with-ai.md` and `substack/articles/drafts/next/2026-11-06-PAID-ATDD-use-cases-stay-user-stories-go.md`. They show a full use case narrative and Gherkin acceptance criteria. The shop has an older, shorter narrative and one-line bullet criteria. A reader who opens the shop after reading either article finds something different from what the article showed.
**End result:** The shop's Place Order narrative and acceptance criteria match what the articles show, the spec gate still passes, and every acceptance criterion has a tagged test.

## Outcomes

- `docs/use-cases/UC-001-place-order.md` holds the narrative exactly as the articles show it, in place of the two-line `## Main scenario`: primary actor Customer, secondary actors ERP and Clock, a 9-step main success scenario, and extensions 2a, 3a, 4a, 4b, 5a, 5b and 6a. The old `docs/design/shared/use-case-narrative.md` is gone, or it points to the new file.
- The narrative says ERP everywhere, never SAP.
- Each acceptance criterion in `docs/use-cases/UC-001-place-order.md` that comes from an extension names that extension: 2a → UC-001.12, 3a → UC-001.13, 4a → UC-001.8, 4b → UC-001.10, 5a → UC-001.14, 5b → UC-001.9, 6a → UC-001.11.
- The acceptance criteria are in the format the articles show (see open question 1).
- UC-001.14 (ERP unavailable) has a tagged Java acceptance test.
- `gh optivem spec` still passes at the `spec.yaml` threshold (90%).

## Open questions

These come first, because the articles and the spec gate disagree. Each one needs a decision before steps 3 and 4.

1. **Gherkin in the use case file breaks the spec gate.** The articles show the acceptance criteria as Gherkin scenarios tagged `@UC-001.N` and `@narrative-4a`. The spec gate reads `docs/use-cases/*.md` with `criterionPattern` in `gh-optivem/internal/spec/usecase.go`, which only matches bullet lines (`- UC-001.N: text`). If we rewrite UC-001 as Gherkin, the gate finds no criteria.
   - **A (recommended):** extend the gh-optivem parser so it also reads Gherkin scenarios tagged `@UC-NNN.M` (`storymerge.go` already parses Gherkin scenarios for stories, so the logic exists), then rewrite UC-001 in Gherkin. The shop then matches the articles, and stories and use cases share one format.
   - B: keep bullets in the shop and change both articles to show bullets. Cheaper, but the articles lose the concrete Given/When/Then examples.
2. **Deleting criteria once their tests pass defeats the spec gate.** The use-cases draft says to delete an acceptance criterion once its test passes, and to generate it from the tests on demand (commit `44cf6ac` in substack). The spec gate works the other way round: it measures how many criteria in `docs/use-cases/` have a passing tagged test, so a deleted criterion drops out of the gate.
   - **A (recommended):** keep the criteria in the shop and change the "delete" paragraph in the use-cases draft (and the matching SHOP WORK notes in both drafts). The gate is what makes the criteria enforceable: the pipeline fails when one has no passing test.
   - B: keep the article and change the gate so that a tagged test counts as the criterion. That's a bigger gh-optivem change and removes the coverage check.
3. **.NET and TypeScript tests have no `UC-` tags.** Only the Java tests are tagged, and `spec.yaml` reads only Java results. The articles show Java only, so this is outside this plan's scope. **Recommended:** leave it out and record it in `plans/backlog/` if it matters.

## Steps

- [ ] Step 1: Write the narrative into `docs/use-cases/UC-001-place-order.md`, replacing the two-line `## Main scenario`. Copy the narrative block verbatim from the legacy coverage draft (the `# Use Case: Place Order` block under "Step 1.2. Generate the use case narrative for the core use case"). Also add `docs/business-rules/UC-001-place-order.md` with BR-1 to BR-4 from the same step. Use the version without Stakeholders, Preconditions, Trigger and guarantees: both drafts dropped those sections on 2026-11-06.
- [ ] Step 2: Remove `docs/design/shared/use-case-narrative.md`, or replace its Place Order part with a link to the new file. Keep the template section only if something still uses it (grep the repo first). Update any links to the old path.
- [ ] Step 3: Rewrite the acceptance criteria in `docs/use-cases/UC-001-place-order.md` in the format decided in open question 1. Keep every ID, UC-001.1 to UC-001.14, unchanged, because the Java tests are tagged with them. Add the narrative references for 2a, 3a, 4b and 6a (see Outcomes).
- [ ] Step 4: If open question 1 is A, the gh-optivem parser change ships first, then this step. If it's B, skip it and change the articles instead.
- [ ] Step 5: Add a Java acceptance test tagged `@Tag("UC-001.14")` for "ERP unavailable": the customer is told product information can't be retrieved right now. It goes in `PlaceOrderNegativeTest` or `PlaceOrderNegativeIsolatedTest`, whichever already stubs the ERP. Check that no test anywhere is tagged `UC-001.14` before adding one.
- [ ] Step 6: Run `gh optivem spec` against the shop and check that the gate passes with all 14 criteria found.
- [ ] Step 7: Tell the substack side which SHOP WORK notes are now done, so they can be removed from both drafts: the narrative note, the Gherkin note, and (depending on open question 2) the "still keeps the acceptance criteria" notes.

## Out of scope

- Gift wrapping, the story-to-criterion mapping, and the illustrative legacy examples (Track Shipment, the wrong draft of 6a, the year-end check). The articles flag these as illustrative SHOP WORK, not as things the shop must have.
- Use cases other than Place Order. The shop has only UC-001.
