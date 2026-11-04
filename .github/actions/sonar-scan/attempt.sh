#!/usr/bin/env bash
# One attempt of a SonarCloud command, run by optivem/actions/retry@v1.
# Records the attempt's combined output in $SONAR_SCAN_LOG (after a marker
# line) so classify.sh can tell an outage from a verdict, while still passing
# the output through for the retry wrapper's own classification and the log.
set -uo pipefail
: "${SONAR_SCAN_COMMAND:?}" "${SONAR_SCAN_LOG:?}"
echo "=== sonar-scan attempt ===" >> "$SONAR_SCAN_LOG"
bash -c "$SONAR_SCAN_COMMAND" 2>&1 | tee -a "$SONAR_SCAN_LOG"
exit "${PIPESTATUS[0]}"
