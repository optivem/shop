# 2026-10-16 11:17:48 UTC — Align test order across shop, gh-optivem and courses with the environment model

## TL;DR

**Why:** The reply to student question optivem/hub#203 sets out which tests run in which environment, and in what order. In the Acceptance Stage it runs **Contract Tests before Acceptance Tests**, because the Acceptance Tests rely on the stubs, so the stubs must be validated first. It also runs Contract Tests before E2E. Shop's acceptance-stage workflows and the course currently run Acceptance before Contract. The course's `03-system-tests-run.md` snippet is also out of date: it shows `./gradlew` with `-Denvironment`, but shop now uses `gh optivem system-test run --suite`.
**End result:** Shop's workflows and `tests.yaml` files, the student repos that gh-optivem creates, and the course lessons all follow the same model (below). A student reading the course, the #203 reply or their own repo sees the same order everywhere.

### Target model (from the #203 reply)

| Where | Deployment | Order |
|---|---|---|
| Local (Docker Compose) | System + External System Stubs | Smoke (external stubs, then system) → Contract (stubs) → Acceptance |
| Acceptance Stage, Deployment A | System + External System Stubs | Smoke (stubs, then system) → Contract (stub, stub-isolated) → Acceptance |
| Acceptance Stage, Deployment B | System + External System Test Instances | Smoke (test instances, then system) → Contract (real) → E2E |
| QA Stage | System + External System Test Instances | Smoke only, then manual QA. Covered by `20261016-1058-qa-stage-smoke-tests.md` |
| Production Stage | System + Real External Systems | Smoke only. Covered by `20261016-1058-qa-stage-smoke-tests.md` |

## Outcomes

- All 12 `*-acceptance-stage.yml` workflows (local and `-cloud`) run the suites in this order: `smoke-stub`, `smoke-real`, `contract-stub`, `contract-stub-isolated`, `contract-real`, then `acceptance-*` ×4, then `e2e-*`. All contract passes, stub and real, run before any acceptance test, so the stubs are checked against the real systems before anything relies on them.
- `system-test/{java,dotnet,typescript}/tests.yaml` list the suites in the same order, so local runs and CI agree.
- Student repos created by gh-optivem get the new order with no hand-editing, and gh-optivem's own docs and tests that list the suite order match it.
- The course matches the model. `01-pipeline/.../03-acceptance-stage/03-system-tests-run.md` lists Contract before Acceptance, explains why, and shows the current `gh optivem system-test run --suite` YAML. The ATDD course's per-module Acceptance Stage lessons don't contradict the order.

## ▶ Next executable step (resume here)

Step 1: in `shop/system-test/java/tests.yaml`, `.github/workflows/monolith-java-acceptance-stage.yml` and gh-optivem's `system-test run` code, find out:
- whether running with no `--suite` runs the suites in `tests.yaml` file order (this decides whether reordering `tests.yaml` changes the Local order)
- whether any suite depends on another having run first

Report back before editing. This unblocks Steps 2–5.

## Steps

- [ ] Step 1: Investigate. Find out how `gh optivem system-test run` orders suites when no `--suite` is given. List every place that hard-codes the suite order: workflows, `tests.yaml`, gh-optivem scaffolding/verify/tests/README, and the courses. Use `grep -rn "contract-stub\|acceptance-parallel"` across shop, gh-optivem and courses.
- [ ] Step 2: shop: reorder the suites in `monolith-java-acceptance-stage.yml` and `system-test/java/tests.yaml`, then check with a CI run. Roll out to the other 11 acceptance-stage workflows (monolith/multitier × java/dotnet/typescript × local/cloud) and the dotnet/typescript `tests.yaml` files. Leave `*-acceptance-stage-legacy.yml` alone, because it mirrors the module-by-module course progression. Step 5 decides whether to change its order too.
- [ ] Step 3: gh-optivem: update anything that lists the suite order (MAPPING.md, README, scaffolding tests, `steps/verify.go`). Create a test student repo for each architecture and language to confirm the copied workflows keep the new order.
- [ ] Step 4: Pipeline course: update `01-pipeline-accelerator/course/03-acceptance-stage/03-system-tests-run.md`:
  - swap the Acceptance and Contract steps in the numbered sequence and add the one-sentence reason (the stubs are validated before the tests that rely on them)
  - replace the `./gradlew ... -Denvironment=acceptance` YAML with the current `gh optivem system-test run --suite` steps
  - fix the "Every test step passes the same `-Denvironment=acceptance` flag" prose so it describes the job-level `ENVIRONMENT` variable instead
  - update the "Check Yourself" answers if they refer to the order
- [ ] Step 5: ATDD course: check the Acceptance Stage lessons in later modules (`11-contract-tests/04-acceptance-stage.md`, `14-atdd-contract-tests/`, `17-architecture-reference/`, the module 10 and 11 recaps) and the `02-smoke-tests/04-acceptance-stage.md` "Sandbox convention" note (its `-Dversion=modNN` / `-Denvironment` wording is out of date). Make any order statements match the model. The legacy per-module snippets stay in introduction order.
- [ ] Step 6: Course prose, optional: add a short "What runs where" summary, the target-model table above, to the Pipeline course (or the Architecture Reference module), so the #203 answer lives in the course.

## Open questions

- **Local order:** the reply currently lists Local as Smoke → Acceptance → Contract, while Deployment A is Smoke → Contract → Acceptance. The target model above assumes Local follows the same reasoning (Contract first). Confirm, or update the #203 reply to match.
- **Contract (real) before Acceptance:** this plan runs all contract passes before Acceptance, so the stubs are checked against reality first. The alternative is to group strictly by deployment: A runs contract-stub then Acceptance, B runs contract-real then E2E. That's closer to the reply's wording but means Acceptance can run on stubs that have drifted from the real systems.
- **Step 6:** is a "What runs where" table wanted in the course, and if so, where?
