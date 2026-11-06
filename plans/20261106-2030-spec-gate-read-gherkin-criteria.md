# 2026-11-06 20:30 UTC — Spec gate: read Gherkin acceptance criteria, then re-enable it in shop

> ⚠️ **Requires a gh-optivem release first.** The parser change is in `optivem/gh-optivem`, not in shop. Do the shop step only after a gh-optivem release that reads Gherkin criteria is out.

## TL;DR

**Why:** On 2026-11-06 shop rewrote `docs/use-cases/UC-001-place-order.md` so the acceptance criteria are Gherkin scenarios tagged `@UC-001.N`, to match the Substack articles (plan `20261106-1533-use-cases-match-substack-articles.md`). The gh-optivem spec parser (`internal/spec/usecase.go`, `criterionPattern`) only reads bullet lines (`- UC-001.N: text`). With Gherkin it finds zero criteria and passes without checking anything: a probe showed `gh optivem spec validate` returns `OK` with exit 0. So shop commented out the `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml`.
**End result:** gh-optivem reads `@UC-NNN.M`-tagged Gherkin scenarios in use-case files. The shop `spec-gate` step is back on and finds all 14 UC-001 criteria.

## ▶ Next executable step (resume here)

Step 1, in a gh-optivem session: extend `internal/spec/usecase.go` so a use case's criteria can come from Gherkin scenarios, reusing the scenario parsing in `internal/spec/storymerge.go`.

## Steps

- [ ] Step 1 — **gh-optivem (other repo):** in `internal/spec/usecase.go`, also read fenced ```` ```gherkin ```` blocks in use-case markdown files. Each `Scenario:` tagged `@UC-NNN.M` becomes a criterion: the ID comes from the tag and the text from the scenario title. Ignore other tags (e.g. `@narrative-4a`), or keep them as metadata. Keep bullet support. Cover duplicate IDs across both formats, a scenario with no UC tag, and a scenario with two UC tags, in `usecase_test.go` (use shop's UC-001 as a testdata fixture). Consider whether `validate` should warn when a use-case file has zero criteria: that's the silent pass that hid this. Publish a release.
- [ ] Step 2 — **shop:** uncomment the `spec-gate` step in `.github/workflows/_acceptance-stage-docker-compose.yml` and delete the DISABLED comment above it.
- [ ] Step 3 — **Verify in shop:** with the new gh-optivem, run `gh optivem spec validate`, then `gh optivem spec status` after a Java acceptance run. Check that all 14 criteria (UC-001.1 to UC-001.14) are found and that the coverage is at least the `spec.yaml` threshold (90%). Push and confirm the monolith-java acceptance stage passes the `spec-gate` step.
