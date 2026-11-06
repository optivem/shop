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

Everything except the manual checks is done and on `main` (2026-11-03). Only the user can run these; delete this plan once they pass. (The teardown dry run passed on 2026-11-06: right GCP/Neon/GitHub targets, no Terraform warnings; with `< /dev/null` it exits at the `read` without printing the prompt.)

1. **Rebuild the devcontainer** (Command Palette → Dev Containers: Rebuild Container) and check that `post-create.sh` installs `neonctl` without sudo errors and reports every tool, including `neonctl` and `jq`, with no `MISSING`.
2. **On a Windows host**, with the `gh.exe` credential helpers in the Windows `~/.gitconfig`: attach to the container and check that `fix-git-credential-helpers.sh` (now `postAttachCommand`) prints "Fixed …" (or "Removed …" while the container's `gh` isn't logged in), and that `git push` works without the `gh.exe: not found` error.
3. Optional: re-run `./setup-gcp.sh` (it's idempotent) and check the "✅ ADC quota project: <project>" line.

**How to resume after the rebuild (user, 2026-11-06):**
1. In a terminal in the rebuilt container, run `neonctl --version` (must print a version) and `git push` (must work, or say "Everything up-to-date", with no `gh.exe: not found`). Scroll back through the rebuild output: no `MISSING` and no sudo errors.
2. Start Claude and type `/execute-plan plans/20261102-2020-sync-gcp-cloud-scripts-from-event-hub.md`. Tell it the results, e.g. "rebuild and Windows git push passed, skip the optional setup-gcp.sh re-run". If something failed, use `/fix-bug <paste the error>` instead.
3. **Executor:** if checks 1 and 2 passed, delete this plan (check 3 is optional) and commit. Also offer the pending `teardown-gcp.sh` fix: on lines 89 and 91, change `read -rp "..." CONFIRM` to `read -rp "..." CONFIRM || CONFIRM=""`. With `set -euo pipefail`, an EOF on stdin (e.g. `< /dev/null`) currently makes the script exit silently with status 1 instead of printing "  Aborted." and exiting 0. Apply it only if the user says yes.

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

The same files have different names in the two repos. Shop's single-pipeline plan (`20261102-1855-single-pipeline-deploy-target.md`, Step 6b) dropped "cloud" from shop's names (done and proven 2026-11-06; that plan is deleted), so match files by this table, not by path. Older notes in this plan still use the old shop names (left column).

| Shop before Step 6b | Shop now | event-hub |
|---|---|---|
| `.github/workflows/_deploy-stage-cloud.yml` | `.github/workflows/_deploy-stage.yml` | `.github/workflows/_deploy-cloud-run.yml` |
| `.github/workflows/_acceptance-stage-cloud.yml` | `.github/workflows/_acceptance-stage.yml` | (in `acceptance-stage.yml`) |
| `.github/actions/deploy-keycloak-cloud` | `.github/actions/deploy-keycloak` | `deploy-keycloak-cloud` |
| `.github/actions/build-cloud-images` | `.github/actions/build-shared-images` | `build-cloud-images` |
| `docker/keycloak/Dockerfile.cloud` | `docker/keycloak/Dockerfile` | `Dockerfile.cloud` |
| `docker/keycloak/cloud-realm.jq` | `docker/keycloak/acceptance-realm.jq` | `cloud-realm.jq` |

Shop's versions also differ in content since Step 6: `build-shared-images` publishes the four shared images to public GHCR (`ghcr.io/<owner>/<repo>/shop-*`) and checks they are anonymously pullable, Cloud Run deploys straight from GHCR, and the Keycloak realms hold `${SYSTEM}`/`${RUN_APP_SUFFIX}` placeholders that Keycloak resolves from the container env (`deploy-keycloak` sets them), so the image takes no build args. Shop also has a billing kill-switch (`terraform/billing-kill-switch.tf`). Compare event-hub's files against these, not against the old shop ones.

## Findings (2026-11-02, shop `main` vs event-hub `main`)

Re-checked 2026-11-03 (Step 0): event-hub moved on in `setup-gcp.sh` (`537a6ad` generates the Keycloak admin password instead of prompting, taken; `4254341`/`835a1f9` print the custom domains, left for the custom-domains plan) and in `.devcontainer/post-create.sh` (`75aec36`, its own version of shop's Claude Code chown, combined with shop's). `teardown-everything.sh` exists only in shop and has none of the three teardown bugs; only its `deploy-config.yml` mention changed. Every other finding below still held.

Source of truth for the teardown/ADC part: event-hub's plan `20261102-2015-sync-gcp-scripts-to-shop.md` (deleted 2026-11-03 once this merge covered it; re-checked then: `teardown-gcp.sh` identical in both repos, `state-backend.sh` differs only in a header comment, `setup-gcp.sh` only in project-specific parts plus event-hub's `CUSTOM_DOMAIN` blocks).

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
- *Reverted (2026-11-03):* `.devcontainer/.gitattributes` was added, then removed because `check-line-endings.yml` requires the root `.gitattributes` to be the single source of truth. The root `* text=auto eol=lf` already keeps `.devcontainer/` LF.
- *Shop-specific, keep:* the name and the `shop-*` volume names, the .NET 8 + PowerShell features and their lock entries, `post-start.sh`'s .NET cleanup, the Playwright path `system-test/typescript`, empty `forwardPorts` (shop has many stacks), the pinned `gh` CLI `2.67.0`, the "set up GCP" hint in the closing message.
- *Event-hub-only:* `vscjava.vscode-java-pack` (shop gets Java through the language feature; add it only if wanted), event-hub's forwarded ports.
- *Shop is ahead (back-port):* the `chown` of the global `claude-code` install so auto-update works, the pinned `gh` version, and possibly the `post-start.sh` approach to the credential helpers.

**Event-hub-only, not ported** (see Decisions)
- Custom-domain mapping (`.github/scripts/ensure-domain-mapping.sh`, the `CUSTOM_DOMAIN` block at the end of `setup-gcp.sh`): goes to the follow-up custom-domains plan.
- Cold-start work (event-hub plan `20261102-1953-reduce-cloud-run-cold-start-latency.md`).

## Steps

- [ ] Step 5 (user only): the manual checks in the ▶ block above.

## Back-port to event-hub (shop is ahead)

Collected while merging (2026-11-03); to hand over to event-hub as its own plan.

- `deploy-keycloak-cloud`: `--min-instances 0` on Keycloak (check it against event-hub's cold-start plan first)
- `.devcontainer/devcontainer.json`: pin the `gh` CLI feature version
- `.devcontainer/fix-git-credential-helpers.sh`: only repoint the helpers to `!gh auth git-credential` when the container's `gh` is logged in; otherwise just drop them, so VS Code's forwarded credentials apply instead of a `gh` that can't answer
- `.devcontainer/post-create.sh`: chown the whole `@anthropic-ai` scope folder, not just the package (npm writes the new version next to it), as shop does since `ad349cf1`. event-hub's `75aec36` already chowns the scope folder and the `bin/claude` link; shop took the link chown from it, so the two are now equal on this point
- `terraform/README.md`: shop's expanded Cost section (`c3fd9b8e`, billed services, what keeps idle cost at ~$0, what would start costing money)

## Decisions (2026-11-02)

- **`deploy-config.yml`: dropped.** It's a gitignored local file that nothing reads, left over from an old Docker-or-Cloud-Run switch, and event-hub has already removed it. Remove the write in `setup-gcp.sh`, teardown's step 4 and its "fall back to Docker deployment" line (teardown goes back to 3 steps, like event-hub's), the mention in `teardown-everything.sh`, the line in `terraform/README.md` and the `.gitignore` entry.
- **Custom domains: yes, in a separate follow-up plan** (decision changed on 2026-11-02, for a fully realistic end-to-end example). Shop uses a subdomain of `optivem.com`, set through the `CUSTOM_DOMAIN` repo variable on `optivem/shop`, so scaffolded repos stay opt-in and default to `*.run.app`. Only one reference variant is mapped (multitier-java QA and prod, plus the shared Keycloak on `auth.`), since it is the only variant with its own QA and production databases. The rest stay on `run.app`. **Where the logic lives:** in the shared `.github/workflows/_deploy-stage-cloud.yml` and `.github/actions/deploy-keycloak-cloud`, not in the 18 variant workflows. The shared workflow takes one optional input (e.g. `frontend-domain`), and only `multitier-java-qa-stage-cloud.yml` (`shop-qa`) and `multitier-java-prod-stage-cloud.yml` (`shop`) pass it. The other 16 variant files don't change. Keycloak is one instance shared by every variant, so moving it to `auth.<domain>` switches the login URL for **all** variants. That happens automatically, through the shared files. That plan waits until event-hub's `20261102-1954-custom-domains-for-cloud-run.md` is proven end to end, including the Keycloak redirect decoupling, then merges the finished design. This plan doesn't port any of it, so `setup-gcp.sh` has no `CUSTOM_DOMAIN` block yet.
- **Cold start:** handled in a follow-up shop plan, once event-hub's `20261102-1953-reduce-cloud-run-cold-start-latency.md` is done.
- **Order:** this sync runs before `20261102-1855-single-pipeline-deploy-target.md`, which then builds on the merged `setup-gcp.sh`, `terraform/main.tf` and `terraform/README.md`.
