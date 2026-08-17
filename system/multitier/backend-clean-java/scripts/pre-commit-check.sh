#!/usr/bin/env bash
#
# Pre-commit check for system/multitier/backend-clean-java
# Runs compile (main + test sources) then checkstyle.
#
set -euo pipefail

cd "$(dirname "$0")/.."

echo "  [java multitier clean] compile..."
./gradlew --quiet compileJava compileTestJava

echo "  [java multitier clean] checkstyle..."
./gradlew --quiet checkstyleMain
