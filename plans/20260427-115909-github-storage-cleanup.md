# Plan — GitHub Storage / Hygiene Cleanup

**Date:** 2026-04-27
**Context:** Follow-on items after adding the `cleanup-ghcr-orphan-manifests` action and wiring `container-packages` into [shop/.github/workflows/cleanup.yml](../.github/workflows/cleanup.yml). The list below covers other GitHub surfaces that accumulate over time and were not addressed by that change.
**Scope:** Highest-ROI items first. Each item is self-contained — apply, leave for later, or drop independently.

## Out of scope

- Items already shipped: tag/release/Docker-image-tag/orphan-manifest cleanup (covered by [actions/cleanup-prereleases](../../actions/cleanup-prereleases/action.yml) + [actions/cleanup-ghcr-orphan-manifests](../../actions/cleanup-ghcr-orphan-manifests/action.yml) + [shop/.github/workflows/cleanup.yml](../.github/workflows/cleanup.yml)).
- Deployments cleanup (already handled by [actions/cleanup-deployments](../../actions/cleanup-deployments/action.yml)).
- Production-release pruning. Final `vX.Y.Z` releases are the historical record; not auto-deleted.

---

## C1 — Auto-delete merged head branches (recommended, one toggle)

**Goal:** Stop the branch-graveyard. After a PR merges, the source branch is deleted automatically.

**Repos affected:** Audit all academy repos. Likely already on for some, off for others. Inventory first.

**Action:**
- For each repo, `gh api repos/<owner>/<repo> --jq .delete_branch_on_merge` to check.
- Where `false`, `gh api -X PATCH repos/<owner>/<repo> -f delete_branch_on_merge=true`.

**Risk:** None. The branch's history is preserved in the merged commit; the ref is just removed. PR authors can restore the branch from the closed PR's UI if needed.

---

## C2 — Workflow run / log retention (org or repo level)

**Goal:** Cap how long Actions runs and their logs hang around. Default is 90 days; for repos with high CI volume (shop in particular) this clutters the Actions UI.

**Action:**
- Decide retention horizon. Recommend **30 days** for shop (long enough to debug a failed pipeline a few weeks later, short enough to keep the UI tidy).
- Set per-repo: `Settings → Actions → General → Artifact and log retention period`.
- Or org-wide: `optivem` org `Settings → Actions → General`.

**Risk:** Low. Old run logs become unavailable after the horizon, but the run record (status, conclusion, commit SHA) stays. If a regression needs older logs, rerun the workflow from the historical commit.

**Decision needed:** retention horizon (recommend 30d) and scope (repo-by-repo vs org default).

---

## C3 — Audit `actions/upload-artifact` retention

**Goal:** Make sure every `upload-artifact` step sets an explicit `retention-days:` shorter than the default 90. Test reports, coverage, and build artifacts shouldn't linger for months.

**Action:**
- `grep -rn "upload-artifact" .github/workflows` across all repos.
- For each step without `retention-days:`, add it (recommend `7` for test reports, `30` for release artifacts).

**Risk:** None — purely shrinks retention.

---

## C4 — Cache budget audit (only if hitting the 10 GB cap)

**Goal:** Confirm `actions/cache` budget isn't being silently evicted, which would make CI slower than it should be.

**Action:**
- For each repo with heavy CI (shop, courses): `gh cache list --repo <repo> --limit 100`.
- If total `>` 9 GB or eviction warnings appear in run logs, prune stale per-tag scopes.
- Only act if there's evidence of eviction; otherwise leave alone.

**Risk:** None.

---

## C5 — `actions/stale` for issues / PRs (defer until needed)

**Goal:** Auto-label and eventually close issues/PRs with no activity for N days.

**Action:**
- Add a workflow using `actions/stale@v9` with conservative thresholds (e.g. 90 days no-activity → `stale` label, +14 days → close).

**Risk:** Medium. Auto-closing genuine bugs nobody got to is annoying. Only do this once issue volume is large enough to need triage help. Probably not yet — skip.

**Decision needed:** wait until an issue/PR backlog actually accumulates; revisit then.

---

## C6 — Stale-branch sweep (one-off, then revisit)

**Goal:** Delete merged or long-abandoned branches that aren't deleted automatically (older PRs predating C1).

**Action:**
- Once C1 is on, do a one-off sweep: `gh api repos/<repo>/branches --paginate --jq '.[] | .name'` → filter to non-default, non-protected, with last commit older than e.g. 60 days → delete.
- Could be a small ad-hoc script in `github-utils/scripts/`. Not worth a recurring workflow.

**Risk:** Low if filter is conservative (only branches whose tip is reachable from the default branch, i.e. truly merged). Higher if blanket-deleting non-merged branches — could lose abandoned-but-meaningful work. Default to merged-only.

---

## C7 — Webhooks / installed apps / deploy-keys audit

**Goal:** Annual sweep to remove integrations from former tooling.

**Action:**
- For each repo: `Settings → Webhooks` and `Settings → Integrations` — eyeball + remove anything dead.
- Org-level: same under `optivem` org settings.

**Risk:** Low. Only act on integrations clearly tied to retired services.

**Decision needed:** schedule for annual run (e.g. every January). Could be a `/schedule` recurring agent.

---

## C8 — Release-asset audit (low priority)

**Goal:** If GH releases have large attached binaries (zips, jars, wheels), retention of those follows release retention. Currently nothing prunes.

**Action:**
- `gh release list --repo <repo>` per repo with attached assets.
- If sizes are significant, decide on a per-major-version retention policy (keep latest N major versions' assets, delete older).
- For shop, releases don't currently attach binaries (Docker images are the artifact). Verify and skip.

**Risk:** None if we leave alone.

---

## C9 — Retired-package sweep (one-off, ad-hoc)

**Goal:** Delete entire GHCR packages that no active workflow publishes to anymore (renamed/retired services). The current `cleanup-prereleases` action only operates on packages listed in `CONTAINER_PACKAGES`; once a package is removed from that list, nothing prunes it — both the package and all its versions linger indefinitely.

**Action:**
- `gh api /orgs/optivem/packages?package_type=container --paginate --jq '.[].name'` to list all GHCR container packages.
- Diff against the union of `CONTAINER_PACKAGES` across all active cleanup workflows.
- For each orphan: confirm no recent pulls (`gh api /orgs/optivem/packages/container/<name>` → `version_count`, `updated_at`), then delete via `gh api -X DELETE /orgs/optivem/packages/container/<name>`.

**Risk:** Medium. Deleting a whole package is irreversible and breaks any external system still pulling it. Always do a dry-run inventory first; only delete after manual confirmation. One-off, not a recurring workflow.

**Decision needed:** confirm no external consumers before deleting any orphan package.
