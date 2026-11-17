# 2026-10-16 10:58:35 UTC — Add smoke tests to the QA and Production Stages

## TL;DR

**Why:** The pipeline course (`courses/01-pipeline-accelerator/course/06-advanced-guides/08-post-deployment-observability.md:48`) says production smoke tests are "the same smoke tests that run in the Acceptance and QA stages", but when this plan was written (2026-10-16) every `*-qa-stage.yml` only deployed and ran no tests. A QA engineer can be handed a deployment that is up but broken, which is the Monday-morning story in the QA Stage overview.
**End result:** Every QA Stage and Production Stage run smoke tests (`smoke-real`) after deployment; a failure stops QA before the deployment is recorded and stops Production before the release is tagged. **Shop's side is delivered** (verified 2026-11-17, see below); its CI verification passed on 2026-11-17 (meta-plan U5), so what is left is the gh-optivem and courses follow-ups (X1). The QA and Production Stage lessons document the step, so the course prose matches the code.

## What is already done in shop (verified 2026-11-17)

The single-pipeline move replaced the 12 per-variant QA/Prod workflows with thin callers of `_qa-stage.yml` and `_prod-stage.yml`, which both deploy through `_deploy-stage.yml`. That workflow ends with a `smoke` job (`needs: [shared-images, deploy-stack]`) that runs `gh optivem system-test run --suite smoke-real` against the deployed Cloud Run URLs. `_qa-stage.yml`'s `record` job and `_prod-stage.yml`'s `release` job both have `needs: [check, deploy]`, so a smoke failure blocks the QA deployment record and the release tag. So the original Steps 1–5 are done, with one design difference: instead of separate `qa` / `production` environments in the system-test configs (old Step 2), the smoke job keeps `ENVIRONMENT: acceptance` and overrides every URL in a generated `systems.yaml`. The smoke tests are read-only `shouldBeRunning()` checks in all three languages (`system-test/{java,dotnet,typescript}` `latest` smoke suites), so running them in Production can't change data.

## Outcomes

- A QA deployment is recorded only after smoke tests confirm the system is reachable in the QA environment, in real external-system mode (QA uses test instances of the real external systems, never stubs; settled 2026-10-16). Done in shop.
- A production release is tagged and published only after the read-only smoke tests pass against the real external systems (settled 2026-10-16). Done in shop.
- Student repos created by gh-optivem carry the same smoke job.
- The `courses/01-pipeline-accelerator/course/04-qa-stage/` and `05-production-stage/` lessons show the smoke-test step. The advanced-guide sentence at `08-post-deployment-observability.md:48` becomes true and no longer presents production smoke tests as optional.

## ▶ Next executable step (resume here)

**Shop is done; only X1 (cross-repo) work remains.** Step 1 (the shop CI verification, meta-plan U5) was confirmed on 2026-11-17 from monolith-java QA-stage run 43509442868 and prod-stage run 43514816632 (both on `71fe5815`, 2026-11-13; `_qa-stage.yml`, `_prod-stage.yml` and `_deploy-stage.yml` are unchanged since): `deploy / smoke` ran `gh optivem system-test run --suite smoke-real` (5/5 passed) and finished before `record` / `release` started, which `needs: [check, deploy]`. The remaining Steps 2 and 3 are X1: per the meta-plan's standing answer they become plan files in gh-optivem and courses; nothing outside shop is edited from here.

## Steps

- [ ] Step 2 (gh-optivem, cross-repo / X1; was Step 6): student repos get these workflows from shop's scaffolding (`internal/scaffolding/steps/apply_template.go`, `pipelineWorkflows`). Check that the scaffold copies the shared `_qa-stage.yml`, `_prod-stage.yml` and `_deploy-stage.yml` (not only the per-variant callers, which are now thin), that `templates.FixupWorkflowContent` rewrites the `smoke` job's lines correctly (the `system-test/<lang>` paths, toolchain setup, `GH_OPTIVEM_CONFIG`), and that `steps/verify.go` doesn't need updating. Then run a test scaffold for each architecture/language.
- [ ] Step 3 (courses, cross-repo / X1; was Step 7): update the `01-pipeline-accelerator/course/04-qa-stage/` lessons (e.g. `01-deploy-to-qa.md` or a new lesson, plus the overview/recap step lists and the sandbox project) to document the smoke-test step. Do the same for `05-production-stage/` (`02-deploy-to-prod.md`, `08-recap.md`, `07-sandbox-project.md`). Rewrite `06-advanced-guides/08-post-deployment-observability.md:48` so production smoke tests are part of the core Production Stage rather than an optional add-on. Describe the step as shop has it (a `smoke-real` run after deploy, URLs pointed at the deployed stage), not with `environment=qa` / `environment=production` flags.
