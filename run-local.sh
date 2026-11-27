#!/usr/bin/env bash
# Quick local run: builds the system from your working copy and starts it in Docker, for clicking
# through a change in the browser. For iterating on the UI, use `dev`: the stack runs in Docker and the
# frontend runs on the Vite dev server with hot reload (save a file, the browser updates), no rebuilds.
# Only the "real" stack (frontend, backend, Postgres, Keycloak, simulators), not the stub one, and no
# tests: for those use `gh optivem system start` / `system-test run` (CONTRIBUTING.md).
#
# Usage:
#   ./run-local.sh                          # up: multitier typescript (the reference variant)
#   ./run-local.sh dev                      # stack in Docker + hot-reloading frontend on :5173 (multitier only)
#   ./run-local.sh up monolith java         # any <architecture> <language>
#   ./run-local.sh logs                     # follow the logs
#   ./run-local.sh down                     # stop (the database volume is kept)
#
# It uses the same compose file and project name as `gh optivem system start` (my-shop-real), so the
# two share containers and data: a later `gh optivem system stop` also stops this one.
set -euo pipefail

action="${1:-up}"
architecture="${2:-multitier}"
language="${3:-typescript}"

case "$action" in up|dev|down|logs) ;; *) echo "Unknown action '$action' (up, dev, down or logs)." >&2; exit 1 ;; esac
case "$architecture" in multitier|monolith) ;; *) echo "Unknown architecture '$architecture' (multitier or monolith)." >&2; exit 1 ;; esac
case "$language" in typescript|java|dotnet) ;; *) echo "Unknown language '$language' (typescript, java or dotnet)." >&2; exit 1 ;; esac

root="$(git rev-parse --show-toplevel)"
dir="${root}/docker/${language}/${architecture}"
compose=(docker compose -p my-shop-real -f "${dir}/docker-compose.local.real.yml")

case "$action" in
  down) "${compose[@]}" down; exit 0 ;;
  logs) "${compose[@]}" logs -f; exit 0 ;;
esac

# The UI URL is the first component of the "real" system in systems.yaml.
ui_url="$(awk '/^ *- label: real/{r=1} r && /url:/{print $2; exit}' "${dir}/systems.yaml")"
[[ -n "$ui_url" ]] || { echo "Cannot find the real UI URL in ${dir}/systems.yaml." >&2; exit 1; }

if [[ "$action" == dev ]]; then
  [[ "$architecture" == multitier ]] || { echo "dev is for multitier only (the monolith serves its own UI)." >&2; exit 1; }
  # Check before the slow Docker build: Keycloak only accepts sign-ins from :5173, so an older dev
  # server still running there (e.g. a plain `npm run dev`, which proxies /api to nothing) blocks this one.
  if curl -s -o /dev/null --max-time 2 http://localhost:5173; then
    echo "Port 5173 is already in use (probably an earlier Vite dev server). Stop it (Ctrl+C in its terminal," >&2
    echo "or end its node process), then run ./run-local.sh dev again." >&2
    exit 1
  fi
fi

echo "Building and starting ${architecture}-${language} from your working copy..."
"${compose[@]}" up -d --build

echo "Waiting for ${ui_url} ..."
up=false
for _ in $(seq 1 90); do
  if curl -fsS -o /dev/null "$ui_url"; then up=true; break; fi
  sleep 2
done
[[ "$up" == true ]] || { echo "Not up after 3 minutes. Check: ./run-local.sh logs" >&2; exit 1; }

if [[ "$action" == up ]]; then
  echo
  echo "Up: ${ui_url}"
  echo "Stop with: ./run-local.sh down"
  exit 0
fi

# dev: Vite dev server on :5173 (allowed in the local Keycloak realm), /api proxied to the backend in Docker.
digit=$(case "$language" in java) echo 1 ;; dotnet) echo 2 ;; typescript) echo 3 ;; esac)
frontend="${root}/system/multitier/frontend-react"
[[ -d "${frontend}/node_modules" ]] || (cd "$frontend" && npm ci)
echo
echo "Dev server: http://localhost:5173 (hot reload; Ctrl+C stops it, the stack keeps running)"
echo "Stop the stack with: ./run-local.sh down"
cd "$frontend"
BACKEND_API_URL="http://localhost:8${digit}11" VITE_KEYCLOAK_URL="http://localhost:8${digit}91" npm run dev -- --strictPort
# --strictPort: Keycloak only accepts sign-ins from :5173, so fail if it is taken (another dev server
# already running) instead of silently moving to :5174, where login would not work.
