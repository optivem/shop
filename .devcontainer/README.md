# Dev Container

Reproducible development environment with `gcloud`, `gh`, `terraform`, Docker, and Node 22 pre-installed. Works identically on Windows, macOS, and Linux.

## Prerequisites

Install once on your host machine:

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows/macOS) or Docker Engine (Linux) — **must be running**
- [VS Code](https://code.visualstudio.com/) with the [Dev Containers extension](https://marketplace.visualstudio.com/items?itemName=ms-vscode-remote.remote-containers)

## Setup

1. Open the repo in VS Code.
2. Command Palette (`Ctrl+Shift+P` / `Cmd+Shift+P`) → **Dev Containers: Reopen in Container**.
3. First launch builds the image (~3-5 min); subsequent launches are instant.
4. [.devcontainer/post-create.sh](post-create.sh) runs automatically and verifies tooling.

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
