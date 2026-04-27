# Plan — Fix SHA-Pinning in Pipeline Deploy

**Date:** 2026-04-27
**Status:** not started
**Owner:** unassigned

## Problem

Every per-(arch, lang) `acceptance-stage`, `qa-stage`, `prod-stage`, and `post-release-stage` workflow advertises a `commit-sha` input that resolves the matching docker image via [optivem/actions/resolve-docker-image-digests@v1](../../actions/resolve-docker-image-digests/action.yml). The resolved digest URL is correctly piped into [tag-docker-images](../../actions/tag-docker-images/action.yml) (which mints the RC tag on the right image) — **but it is silently dropped at deploy time**.

[deploy-docker-compose](../../actions/deploy-docker-compose/action.yml) only echoes the digest in logs:

```bash
echo "📦 Images:"
echo "$IMAGE_URLS" | jq -r '.[]' | while IFS= read -r image_url; do
  echo "   🐳 $image_url"
done
# … then runs plain `docker compose up -d`, which reads the compose file
```

And every `docker-compose.pipeline.*.yml` hardcodes `:latest`:

```yaml
# shop/docker/java/monolith/docker-compose.pipeline.real.yml
services:
  system:
    image: ghcr.io/optivem/shop/monolith-system-java:latest
```

Net effect: `docker compose up` pulls whatever `:latest` points to *now*, not the SHA the user requested. The RC tag goes on the SHA-pinned digest; the tests run against `:latest`. **Stamped artifact ≠ tested artifact whenever those two diverge.**

## Why it matters

- **Hourly cron path (95% of runs):** usually safe — commit-stage publishes `sha-<sha>` and `:latest` simultaneously, so by the time acceptance runs they point at the same digest.
- **Two commits inside one acceptance window:** broken. Commit A triggers acceptance, commit B publishes images mid-flight, acceptance pulls B's `:latest` and RC-tags A's digest.
- **Manual `commit-sha` retry of an old SHA:** definitely broken. User picks `commit-sha: abc123` to re-test 3 commits ago, gets `:latest` (newest), RC-tags `abc123`.
- **Downstream propagation:** `_prerelease-pipeline.yml` and `meta-prerelease-stage.yml` thread `commit-sha` into per-lang acceptance stages — same gap propagates everywhere.

## Out of scope

- The `cross-lang-system-verification.yml` workflow (build-from-source today). Tracked in [its own plan](20260427-130100-cross-language-verification-workflow.md). Will benefit from this fix as a follow-up but does not block it.
- Any `*-stage-cloud.yml` workflows that run elsewhere (GCP). They do not currently use this code path. Re-evaluate once they go live.
- The `docker-compose.local.*.yml` variants — these use `build:` directives, no `image:` pin, no SHA gap. Untouched.

## Scope

### Compose files (6 files in shop)

Switch every pipeline compose file from a hardcoded `:latest` tag to an env-var-substituted reference, with `:latest` as the fallback (preserves cron-path behavior when no SHA is specified).

**Pattern:**
```yaml
# Before
image: ghcr.io/optivem/shop/monolith-system-java:latest

# After (single-image case — monolith)
image: ${SYSTEM_IMAGE:-ghcr.io/optivem/shop/monolith-system-java:latest}

# After (multi-image case — multitier with frontend + backend)
services:
  frontend:
    image: ${SYSTEM_IMAGE_FRONTEND:-ghcr.io/optivem/shop/multitier-frontend-react:latest}
  backend:
    image: ${SYSTEM_IMAGE_BACKEND:-ghcr.io/optivem/shop/multitier-backend-java:latest}
```

**Affected files (12 total — 6 real + 6 stub):**
- `shop/docker/java/monolith/docker-compose.pipeline.{real,stub}.yml`
- `shop/docker/dotnet/monolith/docker-compose.pipeline.{real,stub}.yml`
- `shop/docker/typescript/monolith/docker-compose.pipeline.{real,stub}.yml`
- `shop/docker/java/multitier/docker-compose.pipeline.{real,stub}.yml`
- `shop/docker/dotnet/multitier/docker-compose.pipeline.{real,stub}.yml`
- `shop/docker/typescript/multitier/docker-compose.pipeline.{real,stub}.yml`

Verify each via `grep -n "image:.*:latest" shop/docker/*/*/docker-compose.pipeline.*.yml` returning empty after the edits.

### `deploy-docker-compose` action

Plumb the resolved digest URLs through to compose as env vars.

**Current behavior:** `IMAGE_URLS` env var carries a JSON array of digest URLs, used only for logging.

**New behavior:** Parse the JSON array, derive a service-name → digest map, and export each as `SYSTEM_IMAGE` (single image) or `SYSTEM_IMAGE_<UPPER_SERVICE_NAME>` (multi-image). Pass via the inline `env:` block on the `docker compose` invocation so the substitutions land.

**Service naming convention** — keep simple and explicit:
- Monolith: digest array has one entry → `SYSTEM_IMAGE`
- Multitier: digest array has two entries, ordered `frontend` then `backend` (or alphabetical) → `SYSTEM_IMAGE_FRONTEND`, `SYSTEM_IMAGE_BACKEND`. Add a new optional input `service-names` (newline list) so the caller declares the order/keys explicitly. Default: empty → action falls back to a single `SYSTEM_IMAGE` if exactly one URL was provided, errors otherwise.

**Required input change** — `service-names` (optional, multi-line list). Keep `image-urls` semantics unchanged.

**Backward compatibility** — when `service-names` is empty AND there's one URL, behavior matches the new single-image case. When there's >1 URL and no `service-names`, fail with a clear error rather than guessing.

### Acceptance stage workflows (6 in shop)

No changes required at first — each stage already passes `image-urls: ${{ needs.check.outputs.image-digest-urls }}` to `deploy-docker-compose`. Once the action plumbs them through, behavior is corrected automatically.

For multitier stages, add `service-names: |\n  frontend\n  backend` (or whatever names match the compose service stanza). Without this, the action will error on multi-image inputs.

### Verification

1. Run `monolith-java-acceptance-stage` via `workflow_dispatch` with `commit-sha: <known-old-sha>`. Confirm the deployed container's image reference (`docker compose ps --format json | jq '.[0].Image'`) matches the digest of the old SHA, not whatever `:latest` currently is. Easy way: introduce a deliberate divergence by pushing a no-op image change to bump `:latest`, then re-run with the older `commit-sha` and assert the old digest deployed.
2. Run `multitier-java-acceptance-stage` the same way to exercise multi-image plumbing.
3. Confirm scheduled hourly runs still pass on all 6 stages (`:latest` fallback path).

### Final sweep — no naked `:latest` in pipeline compose

Run these greps as the **last step** before declaring done. Each must return empty.

**1. No naked ghcr.io image references in pipeline compose files.** Every shop image must be wrapped in a `${SYSTEM_IMAGE…:-…}` template. The substitution syntax means the line starts with `image: ${`, so `image: ghcr.io…` (without the `${`) anywhere is a miss:

```bash
grep -rnE 'image:\s+ghcr\.io' shop/docker/*/*/docker-compose.pipeline.*.yml
```
Expected: empty. Any hit is a compose file that wasn't templated.

**2. No naked `:latest` in pipeline compose files.** This catches stragglers where someone left `:latest` outside a `${...}` block, including any non-ghcr image we forgot about. Note that the fallback default inside `${SYSTEM_IMAGE:-...:latest}` is *intentional* — those references are inside `${…}` and the grep below excludes them by requiring `:latest` to appear on a line that doesn't contain `${`:

```bash
grep -rn ':latest' shop/docker/*/*/docker-compose.pipeline.*.yml | grep -v '\${'
```
Expected: empty. Any hit is either (a) a non-templated image still pinning to `:latest`, or (b) a tag literal somewhere unexpected — investigate before closing the plan.

**3. Spot-check one templated file** to confirm the substitution syntax is correct (not e.g. `${SYSTEM_IMGE...}` typo):

```bash
grep -nE 'image:\s+\$\{SYSTEM_IMAGE' shop/docker/java/monolith/docker-compose.pipeline.real.yml
```
Expected: at least one hit, with the var name exactly `SYSTEM_IMAGE` (or `SYSTEM_IMAGE_<SERVICE>` for multitier).

**4. Symmetric check on the action side** — the env vars the action exports must match what the compose files consume. Misspelling here is the silent-failure mode (compose falls back to `:latest`, bug returns invisibly):

```bash
grep -n 'SYSTEM_IMAGE' actions/deploy-docker-compose/
grep -n 'SYSTEM_IMAGE' shop/docker/
```
Eyeball that the two lists contain the same variable names. Any name in one but not the other is a typo or an orphan.

Only after all four greps come back clean is the SHA-pinning fix structurally complete.

## Order of operations

| Order | Step | Reason |
|---|---|---|
| 1 | Update `deploy-docker-compose` action in `actions/` repo, ship as v2 (or extend v1 with backward-compatible new input). | Action change must precede compose-file edits — otherwise compose files reference an env var nothing exports, and `:latest` fallback masks the failure silently. |
| 2 | Edit 12 shop compose files. | Atomic with action update — once action exports `SYSTEM_IMAGE`, compose files consume it. |
| 3 | Add `service-names` input to multitier acceptance stages (3 workflows). | Required for multi-image cases to not fail-error post-action-update. |
| 4 | Verify on one stage manually (step 1 in Verification). | Confirm SHA pinning works end-to-end before declaring done. |
| 5 | Let next hourly cron run on all 6 stages. | Confirm `:latest` fallback still works. |

## Risk

- **Action change is breaking-by-default for multi-image callers without the new input.** Mitigation: ship as v2 of the action, leave v1 alone, callers opt in. Or: keep v1, make `service-names` optional with helpful error message naming what to add.
- **Compose env-var substitution is silent on typos.** If `SYSTEM_IMAGE` is misspelled in either the action or the compose file, the fallback `:latest` kicks in and the bug returns invisibly. Mitigation: post-edit, `grep "SYSTEM_IMAGE" actions/deploy-docker-compose/` and `grep "SYSTEM_IMAGE" shop/docker/` and eyeball that they match.
- **Per-lang stages currently work in 95% of runs.** Don't ship this fix alone if there's appetite to also do the cross-lang workflow refactor — bundle the verification cost.

## Estimate

Half a day, including verification. The mechanical edits are ~30 minutes; designing the `service-names` interface and writing the bash for digest-array parsing is the bulk.
