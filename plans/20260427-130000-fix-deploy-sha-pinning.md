# Plan — Fix SHA-Pinning in Pipeline Deploy

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-27T17:17:07Z`

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

### Compose files (12 files in shop)

Switch every pipeline compose file from a hardcoded `:latest` tag to an env-var reference, with `:latest` as the fallback default. The action exports `SYSTEM_IMAGE_<NAME>=<digest-url>` strictly (it fails loud if `service-names` is missing), so the fallback is rarely exercised in pipeline runs — but it's kept as a safety net for any path that runs compose without going through the action (manual `docker compose up`, ad-hoc debugging, future callers we haven't anticipated).

**Pattern:**
```yaml
# Before
image: ghcr.io/optivem/shop/monolith-system-java:latest

# After (monolith — service stanza named `system`)
services:
  system:
    image: ${SYSTEM_IMAGE_SYSTEM:-ghcr.io/optivem/shop/monolith-system-java:latest}

# After (multitier — service stanzas `frontend` + `backend`)
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

### Verification

1. Run `monolith-java-acceptance-stage` via `workflow_dispatch` with `commit-sha: <known-old-sha>`. Confirm the deployed container's image reference (`docker compose ps --format json | jq '.[0].Image'`) matches the digest of the old SHA, not whatever `:latest` currently is. Easy way: introduce a deliberate divergence by pushing a no-op image change to bump `:latest`, then re-run with the older `commit-sha` and assert the old digest deployed.
2. Run `multitier-java-acceptance-stage` the same way to exercise multi-image plumbing.
3. Confirm scheduled hourly runs still pass on all 6 stages (`:latest` fallback path).

### Final sweep — no literal image tags in pipeline compose

Run these greps as the **last step** before declaring done. Each must return empty (or match the expected pattern).

**1. No naked ghcr.io image references in pipeline compose files.** Every shop image must be wrapped in a `${SYSTEM_IMAGE_<NAME>}` template. The substitution syntax means the line starts with `image: ${`, so `image: ghcr.io…` (without the `${`) anywhere is a miss:

```bash
grep -rnE 'image:\s+ghcr\.io' shop/docker/*/*/docker-compose.pipeline.*.yml
```
Expected: empty. Any hit is a compose file that wasn't templated.

**2. No naked `:latest` outside fallback defaults.** This catches stragglers where someone left `:latest` outside a `${...}` block. Note that the fallback default inside `${SYSTEM_IMAGE_<NAME>:-...:latest}` is *intentional* — those references are inside `${…}` and the grep below excludes them by requiring `:latest` to appear on a line that doesn't contain `${`:

```bash
grep -rn ':latest' shop/docker/*/*/docker-compose.pipeline.*.yml | grep -v '\${'
```
Expected: empty. Any hit is either (a) a non-templated image still pinning to `:latest` directly, or (b) a tag literal somewhere unexpected — investigate before closing the plan.

**3. Spot-check one templated file** to confirm the substitution syntax is correct (not e.g. `${SYSTEM_IMGE...}` typo):

```bash
grep -nE 'image:\s+\$\{SYSTEM_IMAGE' shop/docker/java/monolith/docker-compose.pipeline.real.yml
```
Expected: at least one hit, with the var name exactly `SYSTEM_IMAGE_<SERVICE>` matching the compose service stanza.

**4. Symmetric check** — the service names the workflows pass must match what the compose files consume. Misspelling here is the failure mode (compose substitution resolves to empty, container fails to start):

```bash
grep -rn 'service-names' shop/.github/workflows/
grep -rn '\${SYSTEM_IMAGE' shop/docker/
```
Eyeball that every service name in `service-names` has a matching `${SYSTEM_IMAGE_<UPPER>}` reference in the corresponding compose file. Any name in one but not the other is a typo or orphan.

Only after all four greps come back clean is the SHA-pinning fix structurally complete.

## Order of operations

| Order | Step | Reason |
|---|---|---|
| 1 | Update `deploy-docker-compose` in `actions/` repo: add required `service-names` input, export `SYSTEM_IMAGE_<NAME>=<url>` env vars. ✅ done. | Foundational — every compose change downstream depends on this. |
| 2 | Add `service-names` to all 24 shop workflows that call `deploy-docker-compose`. ✅ done. | The action is now strict; until every caller passes `service-names`, all stages will fail-loud on next run. |
| 3 | Edit 12 shop compose files: replace `:latest` with `${SYSTEM_IMAGE_<NAME>:-...:latest}` (env-var with fallback). | Once workflows pass service-names, env vars are exported; compose files consume them. Fallback is a safety net for paths that bypass the action. |
| 4 | Verify on one stage manually (Verification step 1). | Confirm SHA pinning works end-to-end. |
| 5 | Let next hourly cron run on all 6 stages. | Confirm scheduled path still works. |

## Risk

- **Compose env-var substitution is silent on typos.** If `SYSTEM_IMAGE_<NAME>` is misspelled in either the action input or the compose file, the substitution resolves to empty and the container fails to start (loud — but error message is `image not found ""` which is unintuitive). Mitigation: post-edit, run the symmetric grep in Final sweep #4 and eyeball that workflow service-names match compose env var references.
- **Sequencing window:** between actions push (done) and shop push, every cron-triggered stage will fail. Mitigation: ship steps 2 + 3 in a single shop commit so the window is one push, not multiple.

## Estimate

Half a day, including verification. The mechanical edits are ~30 minutes; designing the `service-names` interface and writing the bash for digest-array parsing is the bulk.
