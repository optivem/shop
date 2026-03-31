"""Single-scenario verification runner — the Python equivalent of verifier.md agent."""

from __future__ import annotations

import secrets
import subprocess
import sys
import time
from dataclasses import dataclass, field

from scaffold.log import fail, log, ok, warn
from scaffold.shell import run
from verify.config import Scenario
from verify.tracking import IssueTracker


@dataclass
class ScenarioResult:
    """Result of running a single verification scenario."""

    scenario_name: str
    passed: bool = False
    step_results: list[str] = field(default_factory=list)
    problems: list[str] = field(default_factory=list)
    fixes: list[str] = field(default_factory=list)
    repo_url: str = ""
    issue_url: str = ""


def _to_kebab(s: str) -> str:
    """Convert 'Page Turner' to 'page-turner'."""
    return s.lower().replace(" ", "-")


def _build_scaffold_args(scenario: Scenario, random_suffix: bool) -> list[str]:
    """Build CLI args for scaffold.py."""
    repo_name = _to_kebab(scenario.system_name)

    args = [
        "--owner", scenario.github_owner,
        "--system-name", scenario.system_name,
        "--repo", repo_name,
        "--arch", scenario.architecture,
    ]

    if scenario.architecture == "monolith":
        if scenario.system_language:
            args.extend(["--lang", scenario.system_language])
    else:
        if scenario.backend_language:
            args.extend(["--backend-lang", scenario.backend_language])
        if scenario.frontend_language:
            args.extend(["--frontend-lang", scenario.frontend_language])

    if scenario.system_test_language:
        args.extend(["--test-lang", scenario.system_test_language])

    if random_suffix:
        args.append("--random-suffix")

    return args


def _get_step_names(arch: str) -> list[str]:
    """Return the list of step names for a given architecture."""
    return [
        "Step 00: Prerequisites",
        "Step 01: Create Repositories",
        "Step 02: Setup Environments",
        "Step 03: Setup Secrets & Variables",
        "Step 04: Clone Repos",
        "Step 05: Apply Template",
        "Step 06: Replace References",
        "Step 07: Replace Namespaces",
        "Step 08: Update README",
        "Step 09: Create SonarCloud Projects",
        "Step 10: Commit & Push",
        "Step 11: Verify Commit Stage",
        "Step 12: Verify Acceptance Stage",
        "Step 13: Verify QA Stage",
        "Step 14: Verify QA Signoff",
        "Step 15: Verify Production Stage",
    ]


def run_scenario(
    scenario: Scenario,
    random_suffix: bool = True,
    cleanup: str = "no",
    dry_run: bool = False,
) -> ScenarioResult:
    """Run a full verification scenario via scaffold.py.

    The scaffold handles all stages (commit, acceptance, QA, QA signoff, prod)
    and reports success/failure via its exit code.
    """
    result = ScenarioResult(scenario_name=scenario.name)
    step_names = _get_step_names(scenario.architecture)
    lang_label = scenario.system_language or scenario.backend_language or "unknown"

    print()
    print("=" * 60)
    print(f"  Scenario: {scenario.name} [{lang_label}, {scenario.architecture}, {scenario.repository_strategy}]")
    print("=" * 60)
    print()

    # --- Step 00: Prerequisites check ---
    log("Step 00: Checking prerequisites...")
    prereq_ok = True
    for tool in ["gh", "git", "python"]:
        check = run(f"which {tool}", check=False, capture=True)
        if check.returncode != 0:
            fail(f"Prerequisite missing: {tool}")
            result.problems.append(f"[Step 00] Missing tool: {tool}")
            prereq_ok = False

    if not prereq_ok:
        result.step_results.append("Step 00: Prerequisites FAIL")
        return result

    result.step_results.append("Step 00: Prerequisites OK")

    # --- Steps 01-15: Run scaffold.py (handles all stages) ---
    log("Running scaffold...")
    scaffold_args = _build_scaffold_args(scenario, random_suffix)

    import os
    scripts_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    scaffold_script = os.path.join(scripts_dir, "scaffold.py")

    if dry_run:
        log(f"[DRY RUN] python {scaffold_script} {' '.join(scaffold_args)}")
        for name in step_names[1:]:
            result.step_results.append(f"{name} OK (dry run)")
        result.passed = True
        return result

    cmd = [sys.executable, scaffold_script] + scaffold_args
    log(f"Running: {' '.join(cmd)}")

    scaffold_result = subprocess.run(cmd, capture_output=True, text=True)

    if scaffold_result.stdout:
        print(scaffold_result.stdout)
    if scaffold_result.stderr:
        print(scaffold_result.stderr)

    if scaffold_result.returncode != 0:
        fail("Scaffold failed!")
        result.problems.append(f"Scaffold exited with code {scaffold_result.returncode}")
        result.step_results.append("Scaffold FAIL")
        return result

    # All scaffold steps passed
    for name in step_names[1:]:
        result.step_results.append(f"{name} OK")

    # Determine the actual repo name
    repo_name = _detect_repo_name(scaffold_result.stdout, scenario)
    full_repo = f"{scenario.github_owner}/{repo_name}"
    result.repo_url = f"https://github.com/{full_repo}"

    # Create tracking issue
    tracker = IssueTracker(full_repo, scenario.name, step_names, dry_run=dry_run)
    try:
        tracker.create()
        for i in range(len(step_names)):
            tracker.complete_step(i)
        tracker.close(success=True)
    except Exception as e:
        warn(f"Could not create tracking issue: {e}")

    if tracker.issue_number:
        result.issue_url = f"https://github.com/{full_repo}/issues/{tracker.issue_number}"

    result.passed = True
    ok(f"Scenario {scenario.name}: ALL STEPS PASSED")

    # Cleanup test repos if requested
    if cleanup == "yes":
        _cleanup_repos(full_repo, scenario, dry_run)

    return result


def _cleanup_repos(full_repo: str, scenario: Scenario, dry_run: bool) -> None:
    """Delete test repos and SonarCloud projects."""
    repos_to_delete = [full_repo]
    if scenario.architecture == "multitier":
        repos_to_delete.append(f"{full_repo}-frontend")
        repos_to_delete.append(f"{full_repo}-backend")

    for repo in repos_to_delete:
        if dry_run:
            log(f"[DRY RUN] Would delete {repo}")
        else:
            run(f"gh repo delete {repo} --yes", check=False)
            ok(f"Deleted repository {repo}")


def _detect_repo_name(scaffold_output: str, scenario: Scenario) -> str:
    """Extract the actual repo name from scaffold output (may include random suffix)."""
    for line in scaffold_output.splitlines():
        if "Repository:" in line and "github.com" in line:
            # Extract repo name from URL like https://github.com/owner/repo-name
            url = line.split("github.com/")[-1].strip()
            parts = url.split("/")
            if len(parts) >= 2:
                return parts[1]

    # Fallback: use kebab system name (scaffold may have added suffix, but we can't detect it)
    return _to_kebab(scenario.system_name)
