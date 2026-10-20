# 2026-10-15 20:40:16 UTC — Fix multi-image preflight gate in acceptance-stage templates

## TL;DR

**Why:** The scheduled `acceptance-stage` workflow in gh-optivem-generated multitier repos fails every hour ([run 38799427976](https://github.com/valentinajemuovic/test-app-34f7c909-fd4670ae81934741/actions/runs/38799427976)) with `manifest unknown` for the frontend `latest` image. The preflight gate uses the any-of `exist` output of `check-ghcr-packages-exist`, so one published image (backend) lets the run proceed while the other (frontend) is still missing.
**End result:** Shop's non-legacy and `-cloud` acceptance-stage templates gate on the all-of `results` output (as the legacy templates already do), so generated repos skip cleanly until every image is published instead of going red.

## Outcomes

- `acceptance-stage` in a generated multitier repo skips with "Not all artifacts published yet" while any image lacks `latest`, and runs once all are present.

## ▶ Next executable step (resume here)

After the shop + actions changes are committed and pushed, verify in a generated multitier repo: trigger `acceptance-stage` (or wait for the hourly cron) in `valentinajemuovic/test-app-34f7c909-fd4670ae81934741` and confirm it **skips** (frontend `latest` still missing), then re-run `frontend-commit-stage` in the `-frontend` repo and confirm the next `acceptance-stage` **runs**. Note: the generated repo has its own copy of the workflow, so it needs re-scaffolding (or a manual 2-line patch) to pick up the fix.

## Steps

- [ ] Step 5: Verify skip-while-incomplete / run-when-complete in a generated multitier repo (see Next executable step).
