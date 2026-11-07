# 2026-11-07 13:00 UTC — gh-optivem `spec` reads `@UC-NNN.M`-tagged Gherkin scenarios as criteria

> ⚠️ **gh-optivem work.** Move this plan into `optivem/gh-optivem`'s `plans/` and run it from a gh-optivem session. Shop never edits gh-optivem. The shop follow-up is Track D of `plans/20261106-1533-use-cases-match-substack-articles.md`; it waits for this plan's release.

## TL;DR

**Why:** Shop's use-case docs (`docs/shared/specs/use-cases/UC-001-place-order/acceptance-criteria.md`) now write acceptance criteria as Gherkin scenarios tagged `@UC-001.N`, to match the Substack articles. gh-optivem's parser (`internal/spec/usecase.go`, `criterionPattern`) reads only bullet lines (`- UC-001.N: text`), so it finds zero criteria and `gh optivem spec validate` passes silently with `OK`, exit 0. Shop has turned its `spec-gate` step off until this is fixed.
Shop is also moving to a folder per use case (see below), which `LoadDir` can't see today: it reads only `.md` files directly inside `specDir`.
**End result:** A published gh-optivem release reads tagged Gherkin scenarios (and bullets) as criteria, in both the flat layout and the folder-per-use-case layout. It warns when a use-case file has no criteria, and finds all 14 criteria in shop's UC-001.

## ▶ Next executable step (resume here)

Step 1: in `internal/spec/usecase.go`, add Gherkin parsing next to the bullet parsing, reusing the scenario parsing in `internal/spec/storymerge.go`.

## What shop has now (input format)

Criteria live in fenced ```` ```gherkin ```` blocks in `docs/shared/specs/use-cases/UC-001-place-order/acceptance-criteria.md`. Each `Scenario:` carries an `@UC-001.N` tag, and extension scenarios also carry a narrative tag (e.g. `@narrative-4a`). There are 14 criteria, UC-001.1 to UC-001.14. Copy the shop file as a test fixture.

**Layout change (decided 2026-11-07, shop plan `plans/20261107-1638-storefront-use-cases-rules-acceptance.md`):** shop is moving its specs to one folder per use case, with `spec.yaml` `specDir: docs/shared/specs/use-cases`:

```
docs/shared/specs/use-cases/
  README.md                     ← index (diagram + table), NO front matter
  UC-001-place-order/
    use-case-narrative.md                 ← narrative, NO front matter
    business-rules.md           ← optional, NO front matter
    acceptance-criteria.md      ← front matter (id, title) + the tagged Gherkin blocks
  UC-002-view-order/
    ...
```

Only `acceptance-criteria.md` is a spec file for gh-optivem. The other files are for people.

Tests already report criteria in the shape `internal/spec/junit.go` reads: per-testcase `<property name="tag" value="UC-001.N"/>`, in Java, .NET and TypeScript JUnit XML. No change is needed there.

## Steps

- [ ] Step 1 — **Parse Gherkin criteria** in `internal/spec/usecase.go`. In fenced `gherkin` blocks, each `Scenario:` (and `Scenario Outline:`) tagged `@UC-NNN.M` becomes one criterion: the ID comes from the tag (without `@`) and the text from the scenario title. Ignore other tags, or keep them as metadata. Keep bullet support unchanged.
- [ ] Step 2 — **Edge cases** in `usecase_test.go`, with shop's UC-001 as a testdata fixture (expect 14 criteria):
  - the same ID in both a bullet and a scenario → a duplicate-ID error, like duplicate bullets today;
  - a scenario with no UC tag → not a criterion (consider a warning);
  - a scenario with two UC tags → decide: error, or two criteria with the same text. Error is the safer choice (one criterion, one scenario).
- [ ] Step 2b — **Folder per use case** in `LoadDir` (`internal/spec/usecase.go`):
  - Also read `<specDir>/*/acceptance-criteria.md`, one level deep, sorted by folder name. Each one is a use case, with its front matter (`id`, `title`) and Gherkin parsed as in Step 1.
  - Skip the other `.md` files inside use-case folders (`use-case-narrative.md`, `business-rules.md`).
  - Don't require front matter on top-level `README.md`. Simplest: skip `README.md` at the top level. A top-level `.md` file with front matter keeps working as today (flat layout), so other repos don't break.
  - Optional check: warn when a folder named `UC-NNN-*` has no `acceptance-criteria.md`, or its front-matter `id` doesn't match the folder's `UC-NNN` prefix.
  - Test with a testdata tree mirroring the layout above: a README with no front matter, one folder with all three files, and one folder without `business-rules.md`. Expect only the criteria from `acceptance-criteria.md` and no front-matter errors.
- [ ] Step 2c — **Skip planned use cases** (requested 2026-11-07 by `plans/20261107-1705-meta-storefront-specs-build.md`, Needs-decision #1): shop writes specs for UC-002 to UC-012 before they are built. Each `acceptance-criteria.md` front matter carries `status: planned | implemented`. `LoadDir` (or the coverage step) must leave `status: planned` use cases out of the criteria count and the threshold, so the gate checks only built use cases. A missing `status` means `implemented` (backward compatible). `spec validate` and `spec status` should still list planned use cases separately (e.g. "UC-006 planned, 17 criteria, not gated"), so they stay visible. Test: a testdata tree with one `implemented` and one `planned` folder; coverage is computed from the implemented one only.
- [ ] Step 3 — **No silent pass:** make `spec validate` warn (or fail) when a use-case file has zero criteria. That silent pass is what hid this problem.
- [ ] Step 4 — **Check pooled results:** shop's Track D will list Java, .NET and TypeScript result globs in `spec.yaml` `results:`. Check how `spec status` merges results across files. If it pools them, one language's passing test hides another language's gap. If so, consider a per-language report (e.g. per-glob coverage) and record the behaviour in this plan's hand-off note so shop can decide.
- [ ] Step 5 — **Release** a new gh-optivem version, and confirm `gh optivem spec validate` in a shop checkout reports 14 criteria. Use whichever layout shop has by then: flat `docs/use-cases/` or `docs/shared/specs/use-cases/UC-001-place-order/acceptance-criteria.md`.

## Hand-off back to shop

Once released, run Track D of `plans/20261106-1533-use-cases-match-substack-articles.md` in shop. Then delete this file from shop's `plans/move-to-gh-optivem/`.
