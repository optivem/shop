# Dev Container

Reproducible development environment with `gcloud`, `gh`, `terraform`, Docker, Node 22, Java 21, .NET 8, PowerShell, and Claude Code pre-installed, plus what the Java, .NET, and TypeScript system tests need (`gh optivem`, Chromium OS libraries). Works identically on Windows, macOS, and Linux.

## Prerequisites

Install once on your host machine:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) or Docker Engine (Linux) — **must be running**
- [VS Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

## Setup

1. Open the `shop/` folder **directly** in VS Code:
   - If a folder or workspace is already open: **File → Close Folder** (or **Close Workspace**) first.
   - **File → Open Folder…** → navigate to and select the `shop/` folder itself (not a parent like `academy/`), then click **Select Folder**.
   - Verify the Explorer sidebar shows `MY_SHOP` at the top and contains `test-all.sh`, `CLAUDE.md`, and `.devcontainer/` at the root. If it shows any other folder name, you opened the wrong level.
   - The `.devcontainer/` must sit at the root of the opened folder — multi-root workspaces and parent directories will not work.
2. Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) → **Dev Containers: Reopen in Container**.
   - ⚠️ Do **not** pick **Dev Containers: Add Dev Container Configuration Files…** — that creates a new config from templates. The repo already ships one in `.devcontainer/`.
3. First launch builds the image (~3-5 min); subsequent launches are instant.
4. [.devcontainer/post-create.sh](post-create.sh) runs automatically and verifies tooling.
5. [.devcontainer/post-start.sh](post-start.sh) runs on every start and replaces any .NET `bin/` and `obj/` folders created on the host, so the C# extension and `dotnet build` work in the container. If you restore or build on the host while the container is running, run `bash .devcontainer/post-start.sh` in the container terminal.

**Troubleshooting:** if you see an **"Add configuration to workspace / user data folder"** prompt or a template picker (Alpine, C#, Show all templates…), VS Code did **not** detect the existing config. Cancel, close the folder, and reopen `shop/` directly — do not pick a template, or you'll overwrite the shared config.

**Troubleshooting (Windows):** if the container fails to start with `accessing specified distro mount service: stat /run/guest-services/distro-services/ubuntu.sock: no such file or directory`, Docker Desktop's WSL integration for your distro (e.g. Ubuntu) isn't running. In Docker Desktop → **Settings → Resources → WSL Integration**, enable your distro and click **Apply & Restart**. If it's already enabled, run `wsl --shutdown`, restart Docker Desktop, and try again. This often breaks after a Docker Desktop update, because the distro integration service doesn't always come back up.

**Troubleshooting (Windows):** if every git command in the container prints `warning: safe.directory 'C:/...' not absolute`, your Windows `~/.gitconfig` has `safe.directory` entries with Windows paths, and Dev Containers copies that file into the container. The warnings are harmless, but the proper fix is on the host. `safe.directory` is only needed for repos owned by a *different* user. Check the folder's owner with `(Get-Acl <folder>).Owner` in PowerShell. If it's your own account, the entries are stale, so remove them on Windows with `git config --global --unset-all safe.directory`. If git on Windows later reports "dubious ownership", fix the ownership with `takeown /f <folder> /r /d y` instead of adding `safe.directory` entries back.

## Deploy to Google Cloud Run

Run these inside the container terminal:

```bash
gh auth login
gcloud auth login
./setup-gcp.sh
gh workflow run monolith-typescript-acceptance-stage-cloud.yml
```

What each step does:
- `gh auth login` — authenticates GitHub CLI (opens browser)
- `gcloud auth login` — authenticates Google Cloud CLI (opens browser)
- `./setup-gcp.sh` — creates GCP project, enables APIs, configures Workload Identity Federation, sets GitHub repo variables/secrets
- `gh workflow run …` — triggers the acceptance-stage workflow against the newly provisioned cloud infra

To tear everything down later: `./teardown-gcp.sh`

## Pinned tool versions

Versions are pinned in [devcontainer.json](devcontainer.json). Update in one place; every dev and CI gets the same env.

| Tool      | Version |
|-----------|---------|
| gh        | 2.67.0  |
| gcloud    | latest  |
| terraform | 1.10.3  |
| node      | 22      |
| claude    | latest  |

## Claude Code

Login state lives in the `shop-claude-config` volume, so it survives rebuilds. `CLAUDE_CONFIG_DIR` points at `~/.claude` so that `.claude.json` (normally in `$HOME`, which is wiped on rebuild) is stored in the volume too. Your host's personal slash commands (`~/.claude/commands`, e.g. `/execute-plan`, `/create-plan`, `/commit`) are bind-mounted read-only on top of it. Edit them on the host; the container sees changes immediately. The host folder must exist before the container starts.

## Rebuilding after config changes

Command Palette → **Dev Containers: Rebuild Container**.
