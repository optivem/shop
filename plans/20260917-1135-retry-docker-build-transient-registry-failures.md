# 2026-09-17 11:35:18 UTC — Retry Docker image builds on transient registry failures

## TL;DR

**Why:** meta-prerelease-stage run 35212665594 failed because child run 35212738296 (`monolith-typescript-commit-stage.yml`) died at "Build and Push Docker Image" on a Docker Hub `502 Bad Gateway` while BuildKit resolved `docker.io/docker/buildkit-syft-scanner:stable-1` (pulled because of `sbom: true`). Every compile/test/lint/pact step had passed. The build step has zero retry, so one registry 5xx fails the whole pipeline.
**End result:** All 7 image-building commit-stage workflows retry the `docker/build-push-action` step once after a short wait, pick up the digest from whichever attempt succeeded, and stay structurally identical across languages. Real build errors still fail loudly (the retry fails too).

## Outcomes

- A single transient registry failure (Docker Hub / GHCR 5xx, network blip) during image build — base-image resolve, SBOM scanner pull, or push — no longer fails a commit stage.
- Genuine Dockerfile / build failures still fail the stage (second attempt fails with the same error, no masking).
- `Compose Digest URL` emits the correct digest whether attempt 1 or attempt 2 produced the image.
- The 7 workflows keep identical step names/shape (no cross-language drift for `workflow-comparator`).

## ▶ Next executable step (resume here)

Step 1: in `.github/workflows/monolith-typescript-commit-stage.yml`, edit the `Build and Push Docker Image` step (`id: push`, ~line 182) to add `continue-on-error: true`; insert after it a `Wait Before Retrying Docker Build` step (`if: steps.push.outcome == 'failure'`, `run: sleep 30`) and a `Build and Push Docker Image (Retry)` step (`id: push-retry`, `if: steps.push.outcome == 'failure'`, identical `uses:`/`with:` block); change `Compose Digest URL`'s `digest:` to `${{ steps.push.outputs.digest || steps.push-retry.outputs.digest }}`. Grep the file for any other `steps.push.` consumer and apply the same coalesce. Gate: YAML parses / actionlint clean. Unblocks Step 2 (replicate to the other 6).

## Steps

- [ ] Step 1: Apply the retry pattern to `monolith-typescript-commit-stage.yml` (see Next executable step). Preserve any existing `if:` on the build step by AND-ing it into the retry step's condition (e.g. the TypeScript/Java/.NET steps may have none today — confirm per file).
- [ ] Step 2: Replicate the identical change in the other 6 workflows: `monolith-java-commit-stage.yml`, `monolith-dotnet-commit-stage.yml`, `multitier-backend-java-commit-stage.yml`, `multitier-backend-dotnet-commit-stage.yml`, `multitier-backend-typescript-commit-stage.yml`, `multitier-frontend-react-commit-stage.yml`. Each retry step's `with:` must mirror that file's own build step (context/file paths differ per project). `multitier-backend-clean-java-commit-stage.yml` builds no image — out of scope.
- [ ] Step 3: Verify: run `actionlint` on the 7 files (fall back to a YAML parse if actionlint isn't installed); diff step names across the 7 files to confirm parity.
- [ ] Step 4: Commit via `/commit`; confirm the next `meta-prerelease-stage` run has all 7 commit stages green.

## Notes (out of scope, record only)

- The existing "Pre-pull base image(s)" steps (`optivem/actions/retry` + `docker pull`) load images into the dockerd store, which the default `docker-container` buildx driver (`setup-buildx-action@v4`) does not use. They act as an early canary for registry health, not as build protection. Left as-is; revisit separately if desired.

## Open questions

- Wait length before retry: 30s inferred (not user-stated). Docker Hub 5xx blips are usually short; bump to 60s if retries are observed still failing.
