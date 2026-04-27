# Dev Container

Reproducible development environment with `gcloud`, `gh`, `terraform`, Docker, and Node 22 pre-installed. Works identically on Windows, macOS, and Linux.

## Prerequisites

Install once on your host machine:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) or Docker Engine (Linux) — **must be running**
- [VS Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

## Setup

1. Open the `shop/` folder **directly** in VS Code:
   - If a folder or workspace is already open: **File → Close Folder** (or **Close Workspace**) first.
   - **File → Open Folder…** → navigate to and select the `shop/` folder itself (not a parent like `academy/`), then click **Select Folder**.
   - Verify the Explorer sidebar shows `MY_SHOP` at the top and contains `run-all-system-tests.sh`, `CLAUDE.md`, and `.devcontainer/` at the root. If it shows any other folder name, you opened the wrong level.
   - The `.devcontainer/` must sit at the root of the opened folder — multi-root workspaces and parent directories will not work.
2. Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) → **Dev Containers: Reopen in Container**.
   - ⚠️ Do **not** pick **Dev Containers: Add Dev Container Configuration Files…** — that creates a new config from templates. The repo already ships one in `.devcontainer/`.
3. First launch builds the image (~3-5 min); subsequent launches are instant.
4. [.devcontainer/post-create.sh](post-create.sh) runs automatically and verifies tooling.

**Troubleshooting:** if you see an **"Add configuration to workspace / user data folder"** prompt or a template picker (Alpine, C#, Show all templates…), VS Code did **not** detect the existing config. Cancel, close the folder, and reopen `shop/` directly — do not pick a template, or you'll overwrite the shared config.

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

## Rebuilding after config changes

Command Palette → **Dev Containers: Rebuild Container**.
