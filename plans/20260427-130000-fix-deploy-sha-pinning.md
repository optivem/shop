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

## Remaining verification

1. Run a multitier acceptance stage (e.g. `multitier-java-acceptance-stage`) end-to-end to exercise multi-image plumbing (`SYSTEM_IMAGE_FRONTEND` + `SYSTEM_IMAGE_BACKEND`). Single-image (monolith) was verified live via `monolith-typescript-acceptance-stage` run [25011526625](https://github.com/optivem/shop/actions/runs/25011526625) — the deploy step's logs show `SYSTEM_IMAGE_SYSTEM=ghcr.io/.../monolith-system-typescript@sha256:ccf797ef...` exported to `$GITHUB_ENV` and consumed by compose. Multi-image plumbing was unit-verified locally (`docker compose config` shows both env vars substituted) but not yet exercised live.
2. Confirm next scheduled hourly runs pass on all 6 stages (passive observation — verifies cron path picks up the new code without regression).
