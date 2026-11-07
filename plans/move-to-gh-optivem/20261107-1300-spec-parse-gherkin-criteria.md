# 2026-11-07 13:00 UTC — gh-optivem `spec` reads `@UC-NNN.M`-tagged Gherkin scenarios as criteria

> ⚠️ **gh-optivem work.** Move this plan into `optivem/gh-optivem`'s `plans/` and run it from a gh-optivem session. Shop never edits gh-optivem. The shop follow-up is Track D of `plans/20261106-1533-use-cases-match-substack-articles.md`; it waits for this plan's release.

## TL;DR

**Why:** Shop's use-case docs (`docs/use-cases/UC-001-place-order.md`) now write acceptance criteria as Gherkin scenarios tagged `@UC-001.N`, to match the Substack articles. gh-optivem's parser (`internal/spec/usecase.go`, `criterionPattern`) reads only bullet lines (`- UC-001.N: text`), so it finds zero criteria and `gh optivem spec validate` passes silently with `OK`, exit 0. Shop has turned its `spec-gate` step off until this is fixed.
**End result:** A published gh-optivem release reads tagged Gherkin scenarios (and bullets) as criteria, warns when a use-case file has no criteria, and finds all 14 criteria in shop's UC-001.

## ▶ Next executable step (resume here)

Step 1: in `internal/spec/usecase.go`, add Gherkin parsing next to the bullet parsing, reusing the scenario parsing in `internal/spec/storymerge.go`.

## What shop has now (input format)

Criteria live in fenced ```` ```gherkin ```` blocks in `docs/use-cases/UC-001-place-order.md`. Each `Scenario:` carries an `@UC-001.N` tag, and extension scenarios also carry a narrative tag (e.g. `@narrative-4a`). There are 14 criteria, UC-001.1 to UC-001.14. Copy the shop file as a test fixture.

Tests already report criteria in the shape `internal/spec/junit.go` reads: per-testcase `<property name="tag" value="UC-001.N"/>`, in Java, .NET and TypeScript JUnit XML. No change is needed there.

## Steps

- [ ] Step 1 — **Parse Gherkin criteria** in `internal/spec/usecase.go`. In fenced `gherkin` blocks, each `Scenario:` (and `Scenario Outline:`) tagged `@UC-NNN.M` becomes one criterion: the ID comes from the tag (without `@`) and the text from the scenario title. Ignore other tags, or keep them as metadata. Keep bullet support unchanged.
- [ ] Step 2 — **Edge cases** in `usecase_test.go`, with shop's UC-001 as a testdata fixture (expect 14 criteria):
  - the same ID in both a bullet and a scenario → a duplicate-ID error, like duplicate bullets today;
  - a scenario with no UC tag → not a criterion (consider a warning);
  - a scenario with two UC tags → decide: error, or two criteria with the same text. Error is the safer choice (one criterion, one scenario).
- [ ] Step 3 — **No silent pass:** make `spec validate` warn (or fail) when a use-case file has zero criteria. That silent pass is what hid this problem.
- [ ] Step 4 — **Check pooled results:** shop's Track D will list Java, .NET and TypeScript result globs in `spec.yaml` `results:`. Check how `spec status` merges results across files. If it pools them, one language's passing test hides another language's gap. If so, consider a per-language report (e.g. per-glob coverage) and record the behaviour in this plan's hand-off note so shop can decide.
- [ ] Step 5 — **Release** a new gh-optivem version, and confirm `gh optivem spec validate` in a shop checkout reports 14 criteria.

## Hand-off back to shop

Once released, run Track D of `plans/20261106-1533-use-cases-match-substack-articles.md` in shop. Then delete this file from shop's `plans/move-to-gh-optivem/`.
