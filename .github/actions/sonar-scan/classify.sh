#!/usr/bin/env bash
# Decides what a failed SonarCloud step means; see action.yml.
# Inputs (env): OUTCOME, SONAR_SCAN_LOG, LABEL, RETRY_ATTEMPTS, GITHUB_OUTPUT,
# GITHUB_STEP_SUMMARY.
set -euo pipefail

marker='=== sonar-scan attempt ==='
# An HTTP 5xx from the server, in the phrasings the scanners use: the scanner
# engine ("Error 500 on https://..."), the JS bootstrapper ("Request failed
# with status code 502"), and plain HTTP status text.
server_error_re='Error 5[0-9][0-9] on https://|HTTP 5[0-9][0-9]|status code 5[0-9][0-9]|received unexpected HTTP status:? 5[0-9][0-9]|Internal Server Error|Bad Gateway|Service Unavailable|Gateway Timeout'

if [[ "$OUTCOME" == "success" ]]; then
  echo "skipped=false" >> "$GITHUB_OUTPUT"
  exit 0
fi

attempts=0
last=""
if [[ -f "$SONAR_SCAN_LOG" ]]; then
  attempts=$(grep -cxF "$marker" "$SONAR_SCAN_LOG" || true)
  # Output of the last attempt only: everything after the final marker line.
  last=$(awk -v m="$marker" '$0 == m { buf = ""; next } { buf = buf $0 "\n" } END { printf "%s", buf }' "$SONAR_SCAN_LOG")
fi

if (( attempts >= RETRY_ATTEMPTS )) && grep -Eq "$server_error_re" <<<"$last"; then
  snippet=$(grep -Em1 "$server_error_re" <<<"$last" | tr -d '`\r' | cut -c1-200 || true)
  echo "::warning title=SonarCloud analysis skipped::${LABEL}: SonarCloud returned a server error after ${attempts} attempts (${snippet}). Treating it as an outage: the stage continues without analysis for this commit."
  {
    echo "### Warning: SonarCloud analysis skipped"
    echo
    echo "**${LABEL}** failed with a SonarCloud server error (\`${snippet}\`) after ${attempts} attempts, so it was treated as a SonarCloud outage and the stage continued."
    echo "This commit has **no Sonar analysis**. Re-run the job once SonarCloud is back if you need it."
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
  echo "skipped=true" >> "$GITHUB_OUTPUT"
  exit 0
fi

echo "::error::${LABEL} failed (attempts: ${attempts}). This is not a SonarCloud outage (that needs the retry wrapper to give up after ${RETRY_ATTEMPTS} attempts on an HTTP 5xx), so the step fails. See the output above."
exit 1
