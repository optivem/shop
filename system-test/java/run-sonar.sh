#!/usr/bin/env bash
# Runs SonarScanner analysis on the Java system tests.
#
# Local helper that pushes a SonarCloud analysis using your personal token.
# CI runs the same analysis (auto-retried in CI via optivem/actions) from monolith-java-acceptance-stage.yml and
# multitier-java's acceptance stages (_acceptance-stage-docker-compose.yml and
# _acceptance-stage.yml) after tests finish; this script is for manual runs.
# Project key: optivem_shop-tests-java (config in build.gradle).
# Get token: https://sonarcloud.io/account/security
#
# Usage: ./run-sonar.sh [TOKEN]
#        SONAR_TOKEN=<t> ./run-sonar.sh

set -euo pipefail

TOKEN="${1:-${SONAR_TOKEN:-}}"
if [ -z "$TOKEN" ]; then
  echo "ERROR: Sonar token required. Set SONAR_TOKEN env var or pass as first arg." >&2
  echo "Get token: https://sonarcloud.io/account/security" >&2
  exit 1
fi

echo "Running SonarScanner for Java system tests..."

./gradlew classes testClasses sonar --info "-Dsonar.token=$TOKEN" -Dsonar.scanner.skipJreProvisioning=true

echo "Sonar analysis complete."
