# 2026-11-02 20:20:00 UTC — Sync the GCP setup/teardown scripts and other cloud pieces from event-hub

## TL;DR

**Why:** event-hub's GCP scripts and devcontainer were ported from shop, and running `./teardown-gcp.sh` in event-hub on 2026-11-02 found three bugs that shop still has. Shop's teardown says the GitHub variables were removed when none were, and running it a second time breaks. event-hub has also gained small improvements since the port (ADC quota-project handling, better Terraform docs) that shop lacks.
**End result:** Every cloud file in shop is a deliberate best-of-both merge of the two repos' versions, not a copy, with recency weighed per file. Shop's `teardown-gcp.sh` and `setup-gcp.sh` carry every relevant event-hub fix, and teardown is idempotent. The shared Terraform helpers and `terraform/README.md` match event-hub, except where shop differs on purpose (variants × languages), and the unused `deploy-config.yml` is gone. Each remaining cloud difference is either ported or written down as shop-specific.

## Outcomes

- `./teardown-gcp.sh` really deletes the GitHub repo variables (no unsupported `--yes` flag on `gh variable delete`)
- If any delete fails, step 3 and the closing banner both say so ("Teardown Complete — with warnings"), instead of a ✅
- Running teardown twice ends with "✅ Nothing to tear down — already clean." and exit 0: no `exit 1`, no Terraform "No outputs found" text leaking into `PROJECT_ID`, and leftover Neon or GitHub pieces still get cleaned up
- The confirmation prompt names the **GCP** project ID (or the system name when there's no project)
- `setup-gcp.sh` and `terraform/state-backend.sh` point Application Default Credentials at the right quota project, so Terraform stops warning and a quota project left over from a torn-down system can't break later calls
- `terraform/README.md` describes what setup actually does today (state backend, Neon, Secret Manager, prerequisites, naming), adapted to shop
- The shop devcontainer has every tool the cloud scripts need (`neonctl`, `jq`) and one working fix for the Windows `gh.exe` credential helpers. It keeps its shop-specific setup (.NET, PowerShell, the .NET `bin/obj` cleanup on start)
- A written list of the cloud files that differ between the two repos *on purpose*, so the next sync doesn't re-investigate them

## ▶ Next executable step (resume here)

Step 0, then Step 1. Refresh the side-by-side comparison: fetch each event-hub file with `gh api repos/optivem/event-hub/contents/<path> -H 'Accept: application/vnd.github.raw'`, re-check both repos' last-commit dates, and classify every hunk as described in the Approach. Then merge event-hub's teardown commits `dd19381`, `e1e1269` and `1d4d5b4` into shop's `teardown-gcp.sh`, hunk by hunk. Don't copy the file over. Do drop shop's step 4, which removes `deploy-config.yml` (see Decisions). Gate: `bash -n teardown-gcp.sh` passes, and `./teardown-gcp.sh < /dev/null` stops at the prompt with the right project, Neon project and variable list.

## Approach: a best-of-both merge, not a copy

For every file, read **both** versions side by side and write the best merged version for shop. Don't take event-hub's file wholesale and don't keep shop's by default.

- **Recency is a strong signal, not the deciding one.** Check `git log` for the file in both repos (dates below). The newer change usually reflects the latest thinking, but read *why* it was made (commit message, linked plan) before adopting it.
- **Go hunk by hunk.** Classify each difference as: (a) a fix or improvement → take it, whichever repo it came from; (b) a shop-specific difference (variants × languages, separate `shop-qa` realm, simulators *and* stubs) → keep shop's; (c) event-hub-specific (its stacks, its devcontainer specifics) → leave out; custom domains → the follow-up plan; (d) both repos changed the same area in different ways → combine them, and note the reasoning in the commit message.
- **Note anything where shop is ahead** (for example `--min-instances 0` on Keycloak) in a short "back-port to event-hub" list, so the two repos converge instead of drifting further. Don't edit event-hub from this plan.
- **Keep shop's style:** comment density, step numbering, emoji banners, wording ("variant" rather than "stack").

Last commit per file as of 2026-11-02:

| File | shop | event-hub | Newer |
|------|------|-----------|-------|
| `setup-gcp.sh` | 2026-10-31 `b80f7463` | 2026-11-02 `3f7d3ad` | event-hub |
| `teardown-gcp.sh` | 2026-10-30 `d03caa14` | 2026-11-02 `1d4d5b4` | event-hub |
| `terraform/state-backend.sh` | 2026-10-30 `92922b28` | 2026-10-31 `c4af57d` | event-hub |
| `terraform/README.md` | 2026-10-31 `b80f7463` | 2026-11-02 `3f7d3ad` | event-hub |
| `terraform/main.tf` | 2026-10-31 `b80f7463` | 2026-10-31 `c4af57d` | same day: compare by content |
| `docker/keycloak/Dockerfile.cloud` | 2026-10-31 `b80f7463` | 2026-10-31 `6889c62` | same day: compare by content |
| `.github/actions/build-cloud-images` | 2026-10-31 `b80f7463` | 2026-10-31 `b70b580` | same day: compare by content |
| `.github/actions/deploy-keycloak-cloud` | 2026-10-31 `b80f7463` | 2026-10-31 `b70b580` | same day; shop has `--min-instances 0` |

| `.devcontainer/devcontainer.json` | 2026-10-30 `7b21f306` | 2026-11-02 `b8b1312` | event-hub |
| `.devcontainer/post-create.sh` | 2026-11-02 `e4652500` | 2026-10-31 `c4af57d` | **shop** |
| `.devcontainer/README.md` | 2026-11-02 `e4652500` | 2026-11-02 `b8b1312` | same day: compare by content |
| `.devcontainer/devcontainer-lock.json` | 2026-10-30 `5587c405` | 2026-10-30 `54319fc` | same day; differs only by shop's extra features |
| `.devcontainer/post-start.sh` (shop) vs `fix-git-credential-helpers.sh` (event-hub) | 2026-11-02 `a623ef9d` | 2026-11-02 `b8b1312` | same day, two different fixes for one problem |

Re-check the dates when executing: either repo may have moved on.

## Shop ↔ event-hub file names

The same files have different names in the two repos. Shop's single-pipeline plan (`20261102-1855-single-pipeline-deploy-target.md`, Step 6b) drops "cloud" from shop's names, so match files by this table, not by path:

| Shop today | Shop after single-pipeline Step 6b | event-hub |
|---|---|---|
| `.github/workflows/_deploy-stage-cloud.yml` | `_deploy-stage.yml` | `.github/workflows/_deploy-cloud-run.yml` |
| `.github/workflows/_acceptance-stage-cloud.yml` | `_acceptance-stage.yml` | (in `acceptance-stage.yml`) |
| `.github/actions/deploy-keycloak-cloud` | `deploy-keycloak` | `deploy-keycloak-cloud` |
| `.github/actions/build-cloud-images` | `build-shared-images` | `build-cloud-images` |
| `docker/keycloak/Dockerfile.cloud` | `Dockerfile` | `Dockerfile.cloud` |
| `docker/keycloak/cloud-realm.jq` | `acceptance-realm.jq` | `cloud-realm.jq` |

## Findings (2026-11-02, shop `main` vs event-hub `main`)

Source of truth for the teardown/ADC part: event-hub's own plan `plans/20261102-2015-sync-gcp-scripts-to-shop.md`.

**To port**
- `teardown-gcp.sh` (173 diff lines): all three bugs are present — `gh "$kind" delete "$name" --yes`, `terraform output -raw project_id/system_name`, `exit 1` on an empty state, and the "Type the project ID to confirm" prompt.
- `terraform/state-backend.sh`: event-hub has `set_adc_quota_project`, called from `use_state_project_for_quota`; `setup-gcp.sh` calls it right after `DB_PREFIX=`.
- `terraform/README.md`: shop's is stale — no state backend, no Neon, no Secret Manager for the Keycloak password, no naming table, still says `terraform init` without backend config and lists `terraform destroy` as the cleanup.
- Small comment/doc touches: `setup-gcp.sh` prerequisite line (`gcloud, gh and neonctl authenticated, and a GCP billing account`).

**Shop-specific — keep as is**
- `setup-gcp.sh` database list (`{multitier,monolith}_{java,dotnet,typescript}_{real,stub}` + multitier-java QA/production), the "Trigger workflow" hint, and comments that say "variant" instead of "stack".
- `docker/keycloak/Dockerfile.cloud`, `cloud-realm.jq`, `.github/actions/build-cloud-images`, `.github/actions/deploy-keycloak-cloud`: they differ only because shop has many variants and a separate `shop-qa` realm and builds simulators *and* stubs. Shop is ahead on one point: `--min-instances 0` on Keycloak.
- `terraform/main.tf` (comments only), `terraform.tfvars.example` (`owner/repo` placeholder), `terraform/neon.sh` (event-hub comment refers to its devcontainer).

**Devcontainer**
- *Take from event-hub:*
  - `neonctl` is installed in `post-create.sh`, with the version pinned by `NEONCTL_VERSION` in `terraform/neon.sh`. Shop's `setup-gcp.sh` uses Neon, but its devcontainer doesn't install it, so today it relies on the `npx` fallback.
  - The `check neonctl` / `check jq` lines and the `neonctl login` hint in the closing message
  - `gcloud auth login --no-launch-browser` in the README
  - The README note that `~/.claude/commands` must exist on the host (shop bind-mounts it too)
  - The Windows troubleshooting note in the README
- *Merge:* the Windows `gh.exe` credential-helper fix. Shop does it in `post-start.sh` (`postStartCommand`, with `waitFor`); event-hub uses a separate `fix-git-credential-helpers.sh` on `postAttachCommand`, plus a README recipe for fixing it on the host. Compare the two, keep one mechanism (shop's `post-start.sh` already has to exist for the .NET `bin/obj` cleanup), and take event-hub's host-side README recipe.
- *Take (decided 2026-11-02):* `.devcontainer/.gitattributes` (`* text eol=lf`, with the comment "These files run inside the Linux container, even on a Windows checkout"). Shop's root `.gitattributes` (`* text=auto eol=lf`) already covers it today, but the local file keeps the devcontainer scripts LF even if the root rules change, and it documents why. It has no CRLF exceptions, which is right because nothing in `.devcontainer/` is a `.bat`/`.cmd` file.
- *Shop-specific, keep:* the name and the `shop-*` volume names, the .NET 8 + PowerShell features and their lock entries, `post-start.sh`'s .NET cleanup, the Playwright path `system-test/typescript`, empty `forwardPorts` (shop has many stacks), the pinned `gh` CLI `2.67.0`, the "set up GCP" hint in the closing message.
- *Event-hub-only:* `vscjava.vscode-java-pack` (shop gets Java through the language feature; add it only if wanted), event-hub's forwarded ports.
- *Shop is ahead (back-port):* the `chown` of the global `claude-code` install so auto-update works, the pinned `gh` version, and possibly the `post-start.sh` approach to the credential helpers.

**Event-hub-only, not ported** (see Decisions)
- Custom-domain mapping (`.github/scripts/ensure-domain-mapping.sh`, the `CUSTOM_DOMAIN` block at the end of `setup-gcp.sh`): goes to the follow-up custom-domains plan.
- Cold-start work (event-hub plan `20261102-1953-reduce-cloud-run-cold-start-latency.md`).

## Steps

- [ ] Step 0: Refresh the comparison (cloud files and `.devcontainer/`). Fetch event-hub's current versions of every file in the table above, re-check the last-commit dates, and diff each one against shop. Classify every hunk (a)–(d) as described in the Approach, and update the Findings if anything changed since 2026-11-02.
- [ ] Step 1: Merge the teardown fixes into `teardown-gcp.sh`. Start from shop's file and work in event-hub's changes (as of `1d4d5b4`) hunk by hunk, keeping anything shop does better:
  - Read Terraform outputs with `terraform output -json` + `jq` (`.project_id.value // empty`), not `-raw`
  - With no state project or no `project_id`, don't exit: fall back to `SYSTEM_NAME` or the repo-derived name (same rule as `terraform/main.tf`) and skip the Terraform step with "(skipped: no GCP project in the Terraform state)"
  - Before confirming, list the GitHub variables that actually exist (`LEFTOVER_VARIABLES`). If there's no project, no Neon project, no variables, print "already clean" and exit 0
  - Prompt: "Type the GCP project ID to confirm"; with no project, ask for the system name
  - Drop `--yes` from `gh variable delete`; set `GH_DELETE_FAILED=1` on a real failure and surface it in step 3 and the banner, with a `gh variable delete <name>` hint
  - Wrap the billing lookup and `terraform destroy` in `if [[ -n "$PROJECT_ID" ]]`, and keep the `cd -` balanced (step 4 relies on it)
- [ ] Step 2: Port `set_adc_quota_project` into `terraform/state-backend.sh`, call it from `use_state_project_for_quota` and from `setup-gcp.sh` after `DB_PREFIX=`. Update the prerequisite comment at the top of `setup-gcp.sh`.
- [ ] Step 3: Check `teardown-everything.sh` for the same bugs (`--yes`, `-raw` outputs, `exit 1` on empty state) and fix it the same way.
- [ ] Step 4: Merge `terraform/README.md`. Take event-hub's newer structure and content (state backend, Neon, prerequisites, naming), and keep or adapt shop's parts: a shop naming table with the variant databases, a setup step list matching shop's merged `setup-gcp.sh`, and teardown via `./teardown-gcp.sh` instead of `terraform destroy`.
- [ ] Step 4b: Go through the remaining same-day files (`terraform/main.tf`, Keycloak cloud files, `build-cloud-images`, `deploy-keycloak-cloud`) hunk by hunk. Take any general improvement from event-hub, and keep the shop-specific parts. Add anything where shop is ahead to the "back-port to event-hub" list at the end of this plan.
- [ ] Step 4c: Merge the devcontainer as listed under Findings → Devcontainer: install neonctl and check it, add the `jq` check, settle on one credential-helper mechanism, add `.devcontainer/.gitattributes`, and merge the README sections. Then run `git add --renormalize .devcontainer` and confirm that nothing changes. Regenerate `devcontainer-lock.json` only if features change.
- [ ] Step 5: Verify: rebuild the devcontainer, and check that `post-create.sh` reports every tool (including `neonctl` and `jq`) and that `git push` works without the `gh.exe` error on a Windows host. `bash -n` on every touched script; `./teardown-gcp.sh < /dev/null` against shop's live project stops at the prompt with the right GCP project, Neon project and variable list and no Terraform warning text. Don't confirm unless a real teardown is wanted. If shop is ever torn down, run teardown again and expect "already clean" + exit 0. Optionally re-run `./setup-gcp.sh` (it's idempotent) to see the ADC line.
- [ ] Step 6: Commit in shop. The message lists the event-hub commits merged in, and every place where shop's version was kept over event-hub's newer one, with the reason.

## Back-port to event-hub (shop is ahead)

Collected while merging; to hand over to event-hub as its own plan.

- `deploy-keycloak-cloud`: `--min-instances 0` on Keycloak (check it against event-hub's cold-start plan first)
- `.devcontainer/post-create.sh`: `chown` the global `claude-code` install so auto-update works
- `.devcontainer/devcontainer.json`: pin the `gh` CLI feature version
- Credential helpers: whichever mechanism wins the Step 4c comparison

## Decisions (2026-11-02)

- **`deploy-config.yml`: dropped.** It's a gitignored local file that nothing reads, left over from an old Docker-or-Cloud-Run switch, and event-hub has already removed it. Remove the write in `setup-gcp.sh`, teardown's step 4 and its "fall back to Docker deployment" line (teardown goes back to 3 steps, like event-hub's), the mention in `teardown-everything.sh`, the line in `terraform/README.md` and the `.gitignore` entry.
- **Custom domains: yes, in a separate follow-up plan** (decision changed on 2026-11-02, for a fully realistic end-to-end example). Shop uses a subdomain of `optivem.com`, set through the `CUSTOM_DOMAIN` repo variable on `optivem/shop`, so scaffolded repos stay opt-in and default to `*.run.app`. Only one reference variant is mapped (multitier-java QA and prod, plus the shared Keycloak on `auth.`), since it is the only variant with its own QA and production databases. The rest stay on `run.app`. **Where the logic lives:** in the shared `.github/workflows/_deploy-stage-cloud.yml` and `.github/actions/deploy-keycloak-cloud`, not in the 18 variant workflows. The shared workflow takes one optional input (e.g. `frontend-domain`), and only `multitier-java-qa-stage-cloud.yml` (`shop-qa`) and `multitier-java-prod-stage-cloud.yml` (`shop`) pass it. The other 16 variant files don't change. Keycloak is one instance shared by every variant, so moving it to `auth.<domain>` switches the login URL for **all** variants. That happens automatically, through the shared files. That plan waits until event-hub's `20261102-1954-custom-domains-for-cloud-run.md` is proven end to end, including the Keycloak redirect decoupling, then merges the finished design. This plan doesn't port any of it, so `setup-gcp.sh` has no `CUSTOM_DOMAIN` block yet.
- **Cold start:** handled in a follow-up shop plan, once event-hub's `20261102-1953-reduce-cloud-run-cold-start-latency.md` is done.
- **Order:** this sync runs before `20261102-1855-single-pipeline-deploy-target.md`, which then builds on the merged `setup-gcp.sh`, `terraform/main.tf` and `terraform/README.md`.
