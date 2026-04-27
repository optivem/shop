#!/usr/bin/env bash
# gh-retry.sh — retry wrapper for `gh` CLI invocations in shop workflows.
#
# Source this file from a `run:` step, then replace `gh ...` with `gh_retry ...`:
#
#   source "$GITHUB_WORKSPACE/.github/workflows/scripts/gh-retry.sh"
#   gh_retry release create "$RELEASE_TAG" --title "$RELEASE_TAG" --notes "..."
#
# On success, stdout/stderr are forwarded to the caller. On transient failure
# (HTTP 5xx, network/DNS/TLS blips, connection resets), the call is retried up
# to 4 times with 5s -> 15s -> 45s backoff. On non-transient failure (4xx,
# auth, validation, 404 existence probes, rate-limit), the wrapper passes
# through the original output and exit code so callers that use exit code for
# flow control (e.g. `gh api .../tags/X` to detect absence) keep working.
#
# Mirrors the policy of optivem/actions/shared/gh-retry.sh — kept separate so
# shop workflows are self-contained.

_GH_RETRY_ATTEMPTS=4
_GH_RETRY_DELAYS=(5 15 45)
_GH_RETRY_RETRYABLE='HTTP 5[0-9][0-9]|timeout|timed out|i/o timeout|connection reset|connection refused|\bEOF\b|was closed|TLS handshake|tls:.*handshake|temporary failure in name resolution|no such host|Bad Gateway|Service Unavailable|Gateway Timeout|server error'
_GH_RETRY_HARD_FAIL='HTTP 4[0-9][0-9]|HTTP 403.*rate limit'

gh_retry() {
    local attempt=1
    local code=0
    local stdout_file stderr_file
    stdout_file=$(mktemp -t gh-retry-out.XXXXXX)
    stderr_file=$(mktemp -t gh-retry-err.XXXXXX)

    while (( attempt <= _GH_RETRY_ATTEMPTS )); do
        : >"$stdout_file"
        : >"$stderr_file"
        gh "$@" >"$stdout_file" 2>"$stderr_file"
        code=$?

        if (( code == 0 )); then
            cat "$stdout_file"
            [[ -s "$stderr_file" ]] && cat "$stderr_file" >&2
            rm -f "$stdout_file" "$stderr_file"
            return 0
        fi

        local stderr_content
        stderr_content=$(cat "$stderr_file")

        if grep -Eqi "$_GH_RETRY_HARD_FAIL" <<<"$stderr_content"; then
            cat "$stdout_file"
            cat "$stderr_file" >&2
            rm -f "$stdout_file" "$stderr_file"
            return "$code"
        fi

        if ! grep -Eqi "$_GH_RETRY_RETRYABLE" <<<"$stderr_content"; then
            cat "$stdout_file"
            cat "$stderr_file" >&2
            rm -f "$stdout_file" "$stderr_file"
            return "$code"
        fi

        local snippet
        snippet=$(head -n1 "$stderr_file" | tr -d '\r')

        if (( attempt < _GH_RETRY_ATTEMPTS )); then
            local delay_idx=$(( attempt - 1 ))
            if (( delay_idx >= ${#_GH_RETRY_DELAYS[@]} )); then
                delay_idx=$(( ${#_GH_RETRY_DELAYS[@]} - 1 ))
            fi
            local sleep_s=${_GH_RETRY_DELAYS[$delay_idx]}
            echo "::notice::[gh-retry] attempt $attempt failed (exit $code): $snippet -- retrying in ${sleep_s}s" >&2
            sleep "$sleep_s"
        else
            echo "::warning::[gh-retry] exhausted $_GH_RETRY_ATTEMPTS attempts (exit $code): $snippet" >&2
            cat "$stdout_file"
            cat "$stderr_file" >&2
            rm -f "$stdout_file" "$stderr_file"
            return "$code"
        fi

        (( attempt++ ))
    done

    rm -f "$stdout_file" "$stderr_file"
    return "$code"
}

# git_push_retry REMOTE REF [REF ...]
#
# Retry `git push REMOTE REF...` on transient network/server failures. Does
# NOT retry on real conflicts (non-fast-forward, rejected, hook decline) since
# those mean another writer beat us — retrying would not help and could mask
# the race.
git_push_retry() {
    local attempt=1 max=3
    local delays=(10 30 60)
    local stderr_file
    stderr_file=$(mktemp -t git-push-retry-err.XXXXXX)
    local code=0

    while (( attempt <= max )); do
        : >"$stderr_file"
        if git push "$@" 2>"$stderr_file"; then
            cat "$stderr_file" >&2
            rm -f "$stderr_file"
            return 0
        fi
        code=$?

        local stderr_content
        stderr_content=$(cat "$stderr_file")

        if grep -Eqi 'rejected|non-fast-forward|forbidden|denied|hook declined|cannot lock ref|protected branch' <<<"$stderr_content"; then
            cat "$stderr_file" >&2
            rm -f "$stderr_file"
            return "$code"
        fi

        if ! grep -Eqi 'unable to access|could not resolve host|connection reset|connection refused|HTTP 5[0-9][0-9]|server error|gateway timeout|early EOF|RPC failed|operation timed out|network is unreachable' <<<"$stderr_content"; then
            cat "$stderr_file" >&2
            rm -f "$stderr_file"
            return "$code"
        fi

        if (( attempt < max )); then
            local sleep_s=${delays[$((attempt - 1))]}
            echo "::notice::[git-push-retry] attempt $attempt failed (exit $code) -- retrying in ${sleep_s}s" >&2
            sleep "$sleep_s"
        else
            echo "::warning::[git-push-retry] exhausted $max attempts (exit $code)" >&2
            cat "$stderr_file" >&2
        fi

        (( attempt++ ))
    done

    rm -f "$stderr_file"
    return "$code"
}
