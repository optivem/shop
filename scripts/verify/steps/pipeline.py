"""Pipeline verification steps: QA stage and Production stage."""

from __future__ import annotations

import json
import time

from scaffold.log import fail, fatal, log, ok
from scaffold.shell import check_rate_limit, run


POLL_INTERVAL = 30
MAX_POLLS = 10


def _workflow_prefix(arch: str, test_lang: str) -> str:
    """Return the workflow filename prefix for a given architecture."""
    if arch == "monolith":
        return f"monolith-{test_lang}"
    return f"multitier-system-{test_lang}"


def _trigger_and_wait(
    repo: str,
    workflow: str,
    label: str,
    inputs: dict[str, str] | None = None,
    dry_run: bool = False,
) -> None:
    """Trigger a workflow_dispatch and poll until completion."""
    if dry_run:
        log(f"[DRY RUN] Would trigger {workflow} and wait")
        return

    input_flags = ""
    if inputs:
        for k, v in inputs.items():
            input_flags += f" -f {k}={v}"

    run(f"gh workflow run {workflow}{input_flags} --repo {repo}")
    log(f"Triggered {label} ({workflow})")
    time.sleep(5)

    for attempt in range(1, MAX_POLLS + 1):
        check_rate_limit()
        result = run(
            f"gh run list --repo {repo} --workflow {workflow} --limit 1 --json status,conclusion,databaseId",
            capture=True,
        )
        runs = json.loads(result.stdout)
        if not runs:
            log(f"  [{attempt}/{MAX_POLLS}] No runs found yet...")
            time.sleep(POLL_INTERVAL)
            continue

        r = runs[0]
        status = r.get("status", "")
        conclusion = r.get("conclusion", "")

        if status == "completed":
            if conclusion == "success":
                ok(f"{label} passed!")
                return
            else:
                fail(f"{label} failed with conclusion: {conclusion}")
                fatal(f"{label} workflow failed. Check: https://github.com/{repo}/actions")

        log(f"  [{attempt}/{MAX_POLLS}] {label} status: {status}")
        time.sleep(POLL_INTERVAL)

    fail(f"{label} timed out after {MAX_POLLS} polls")
    fatal(f"{label} workflow timed out. Check: https://github.com/{repo}/actions")


def _get_latest_rc_version(repo: str, dry_run: bool = False) -> str:
    """Get the latest RC release tag from the repo."""
    if dry_run:
        return "v1.0.0-rc.1"

    check_rate_limit()
    result = run(
        f'gh release list --repo {repo} --limit 10 --json tagName,isPrerelease',
        capture=True,
    )
    releases = json.loads(result.stdout)

    for release in releases:
        tag = release["tagName"]
        if "-rc." in tag:
            ok(f"Found RC release: {tag}")
            return tag

    fatal(f"No RC release found in {repo}")
    return ""  # unreachable


def verify_qa_stage(
    repo: str,
    arch: str,
    test_lang: str,
    dry_run: bool = False,
) -> str:
    """Verify QA stage: trigger qa-stage, check qa-deployed, run qa-signoff, check qa-approved.

    Returns the RC version used.
    """
    log("Verifying QA stage...")
    prefix = _workflow_prefix(arch, test_lang)

    # Get the RC version from acceptance stage
    rc_version = _get_latest_rc_version(repo, dry_run)

    # Trigger QA stage
    _trigger_and_wait(
        repo=repo,
        workflow=f"{prefix}-qa-stage.yml",
        label="QA stage",
        inputs={"version": rc_version},
        dry_run=dry_run,
    )

    # Verify qa-deployed release exists
    if not dry_run:
        result = run(
            f'gh release list --repo {repo} --limit 10 --json tagName',
            capture=True,
        )
        releases = json.loads(result.stdout)
        qa_deployed = any("qa-deployed" in r["tagName"] for r in releases)
        if qa_deployed:
            ok("QA deployed release found")
        else:
            fail("QA deployed release not found")
            fatal(f"Expected -qa-deployed release in {repo}")

    # Trigger QA signoff
    _trigger_and_wait(
        repo=repo,
        workflow=f"{prefix}-qa-signoff.yml",
        label="QA signoff",
        inputs={"version": rc_version, "result": "approved"},
        dry_run=dry_run,
    )

    # Verify qa-approved release exists
    if not dry_run:
        result = run(
            f'gh release list --repo {repo} --limit 10 --json tagName',
            capture=True,
        )
        releases = json.loads(result.stdout)
        qa_approved = any("qa-approved" in r["tagName"] for r in releases)
        if qa_approved:
            ok("QA approved release found")
        else:
            fail("QA approved release not found")
            fatal(f"Expected -qa-approved release in {repo}")

    ok("QA stage verified!")
    return rc_version


def verify_production_stage(
    repo: str,
    arch: str,
    test_lang: str,
    rc_version: str,
    dry_run: bool = False,
) -> None:
    """Verify Production stage: trigger prod-stage, verify final release."""
    log("Verifying Production stage...")
    prefix = _workflow_prefix(arch, test_lang)

    # Trigger production stage
    _trigger_and_wait(
        repo=repo,
        workflow=f"{prefix}-prod-stage.yml",
        label="Production stage",
        inputs={"version": rc_version},
        dry_run=dry_run,
    )

    # Verify final release (no -rc suffix) is marked as Latest
    if not dry_run:
        # Extract base version from RC (e.g. v1.0.0-rc.1 -> v1.0.0)
        base_version = rc_version.split("-rc.")[0]

        result = run(
            f'gh release list --repo {repo} --limit 10 --json tagName,isLatest',
            capture=True,
        )
        releases = json.loads(result.stdout)
        final = next((r for r in releases if r["tagName"] == base_version), None)
        if final:
            ok(f"Final release found: {base_version}")
            if final.get("isLatest"):
                ok("Final release is marked as Latest")
            else:
                fail(f"Final release {base_version} is NOT marked as Latest")
                fatal(f"Expected {base_version} to be Latest in {repo}")
        else:
            fail(f"Final release {base_version} not found")
            fatal(f"Expected final release {base_version} in {repo}")

    ok("Production stage verified!")
