# 2026-10-16 11:17:48 UTC — Align test order across shop, gh-optivem and courses with the environment model

## TL;DR

**Why:** The reply to student question optivem/hub#203 sets out which tests run in which environment, and in what order. In the Acceptance Stage it runs **Contract Tests before Acceptance Tests**, because the Acceptance Tests rely on the stubs, so the stubs must be validated first. It also runs Contract Tests before E2E. Shop's acceptance-stage workflows and the course currently run Acceptance before Contract in CI. The course's `03-system-tests-run.md` snippet is also out of date: it shows `./gradlew` with `-Denvironment`, but shop now uses `gh optivem system-test run --suite`.
**End result:** Shop's CI acceptance-stage workflows, the student repos that gh-optivem creates, and the course lessons all run every Contract pass before any Acceptance test (model below). The **local** order (`tests.yaml`, a plain `gh optivem system-test run`) stays as it is today: Smoke → Acceptance → Contract → E2E.

### Target model

Decided (user, 2026-11-16, meta-plan G0 answer 6): only CI changes; Local keeps its current order.

| Where | Deployment | Order |
|---|---|---|
| Local (Docker Compose, `tests.yaml` order) | System + External System Stubs | **unchanged:** Smoke → Acceptance → Contract → E2E |
| CI Acceptance Stage (`_acceptance-stage-docker-compose.yml` latest suite set, and cloud `_acceptance-stage.yml`) | System + stubs, and System + test instances | Smoke (`smoke-stub`, `smoke-real`) → **all** Contract (`contract-stub`, `contract-stub-isolated`, `contract-real`) → Acceptance ×4 → E2E |
| QA Stage | System + External System Test Instances | Smoke only, then manual QA. Covered by `20261016-1058-qa-stage-smoke-tests.md` |
| Production Stage | System + Real External Systems | Smoke only. Covered by `20261016-1058-qa-stage-smoke-tests.md` |

## Outcomes

- The two reusable acceptance workflows run the latest suites in this order: `smoke-stub`, `smoke-real`, `contract-stub`, `contract-stub-isolated`, `contract-real`, then `acceptance-*` ×4, then `e2e-*`. All contract passes, stub and real, run before any acceptance test, so the stubs are checked against the real systems before anything relies on them. (The 18 per-variant `*-acceptance-stage*.yml` callers, latest and legacy, are now thin callers of these two and list no suites.)
- `system-test/{java,dotnet,typescript}/tests.yaml` and the system-test READMEs are **not** reordered: Local keeps Smoke → Acceptance → Contract → E2E.
- The legacy suite set (`tests.legacy.yaml`, the `suite-set == 'legacy'` steps) keeps its module-by-module introduction order.
- Student repos created by gh-optivem get the new CI order with no hand-editing, and gh-optivem's own docs and tests that list the CI suite order match it.
- The course matches the model. `01-pipeline/.../03-acceptance-stage/03-system-tests-run.md` lists Contract before Acceptance, explains why, and shows the current `gh optivem system-test run --suite` YAML. The ATDD course's per-module Acceptance Stage lessons don't contradict the order.

## ▶ Next executable step (resume here)

Meta-plan unit U6 (Lane W, after U4/B-PIN is pushed: same files, Conflict 2). **Step 2:** in `.github/workflows/_acceptance-stage-docker-compose.yml` (the `suite-set == 'latest'` steps, currently lines ~389–423) and `.github/workflows/_acceptance-stage.yml` (lines ~731–755), move the `# === Run Contract Tests ===` block (`contract-stub`, `contract-stub-isolated`, `contract-real`) above the `# === Run Acceptance Tests ===` block. Touch nothing else. Then dispatch one docker-compose acceptance caller (e.g. `monolith-java-acceptance-stage-docker-compose.yml`) and one cloud caller and confirm green.

## Steps

Step 1 (shop part) is done by this review (2026-11-17): in shop, only the two reusable workflows hard-code the CI order (`grep -rln "contract-stub\|acceptance-parallel"` outside `plans/` hits `_acceptance-stage.yml`, `_acceptance-stage-docker-compose.yml`, and the `system-test/*/tests*.yaml` + READMEs, which stay unchanged). Each CI step runs one `--suite`, so file order in `tests.yaml` doesn't matter to CI.

- [ ] Step 2: shop: reorder the latest suite steps in `_acceptance-stage-docker-compose.yml` and `_acceptance-stage.yml` as above, then check with a CI run of one docker-compose caller and one cloud caller. Leave the legacy suite-set steps and every `tests*.yaml` alone.
- [ ] Step 3 (gh-optivem, cross-repo / X1): update anything that lists the CI suite order (MAPPING.md, README, scaffolding tests, `steps/verify.go`); find them with `grep -rn "contract-stub\|acceptance-parallel"`. Create a test student repo for each architecture and language to confirm the copied workflows keep the new order. Don't change any local-order listing.
- [ ] Step 4 (courses, cross-repo / X1): Pipeline course: update `01-pipeline-accelerator/course/03-acceptance-stage/03-system-tests-run.md`:
  - swap the Acceptance and Contract steps in the numbered sequence and add the one-sentence reason (the stubs are validated before the tests that rely on them)
  - replace the `./gradlew ... -Denvironment=acceptance` YAML with the current `gh optivem system-test run --suite` steps
  - fix the "Every test step passes the same `-Denvironment=acceptance` flag" prose so it describes the job-level `ENVIRONMENT` variable instead
  - update the "Check Yourself" answers if they refer to the order
- [ ] Step 5 (courses, cross-repo / X1): ATDD course: check the Acceptance Stage lessons in later modules (`11-contract-tests/04-acceptance-stage.md`, `14-atdd-contract-tests/`, `17-architecture-reference/`, the module 10 and 11 recaps) and the `02-smoke-tests/04-acceptance-stage.md` "Sandbox convention" note (its `-Dversion=modNN` / `-Denvironment` wording is out of date). Make any CI-order statements match the model; local-run descriptions keep the current order. The legacy per-module snippets stay in introduction order.

## Resolved decisions

All decided (user, 2026-11-16, meta-plan G0 answer 6):

- **Local order:** keep it as-is (Smoke → Acceptance → Contract → E2E). Only CI moves Contract before Acceptance. `tests.yaml` is not reordered and the #203 reply's Local row stays correct.
- **Contract (real) before Acceptance:** yes, in CI all contract passes (stub, stub-isolated, real) run before Acceptance, so Acceptance never runs on stubs that have drifted from the real systems.
- **Step 6 ("What runs where" course table):** skipped and deleted; the target-model table already lives in the #203 reply.
