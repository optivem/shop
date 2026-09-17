#!/usr/bin/env bash
#
# Pre-commit check for system/multitier/frontend-react
#
set -euo pipefail

cd "$(dirname "$0")/.."

echo "  [frontend-react] typecheck..."
npx --no-install tsc --noEmit

echo "  [frontend-react] lint..."
npx --no-install eslint . --max-warnings 0
