"""Shell and API helpers: GitHub CLI wrapper, SonarCloud client, subprocess runner."""

from __future__ import annotations

import base64
import json
import subprocess
import time
import urllib.error
import urllib.request

from .config import Config
from .log import log, fatal, ok, warn


# ─── Rate limit settings ──────────────────────────────────────────────────────
# GitHub API limits:
# - Personal access token: 5,000 requests/hour
# - GITHUB_TOKEN in Actions: 1,000 requests/hour/repo
# Reference: https://docs.github.com/en/rest/using-the-rest-api/rate-limits-for-the-rest-api

RATE_LIMIT_THRESHOLD = 50


# ─── Rate limit helper ─────────────────────────────────────────────────────


class RateLimitExceeded(Exception):
    """Raised when a gh command fails due to GitHub API rate limiting."""


def check_rate_limit(threshold: int = RATE_LIMIT_THRESHOLD) -> None:
    """Check GitHub API rate limit and wait if remaining is below threshold."""
    result = subprocess.run(
        "gh api rate_limit --jq '.resources.core'",
        shell=True, check=False, capture_output=True, text=True,
    )
    if result.returncode != 0:
        return
    try:
        data = json.loads(result.stdout)
    except json.JSONDecodeError:
        return
    remaining = data.get("remaining", 999)
    if remaining < threshold:
        reset_ts = data.get("reset", 0)
        wait_secs = max(reset_ts - int(time.time()) + 5, 0)
        if wait_secs > 0:
            log(f"Rate limit low ({remaining} remaining). Waiting {wait_secs}s for reset...")
            time.sleep(wait_secs)
        else:
            log(f"Rate limit low ({remaining} remaining) but reset is imminent.")


# ─── Subprocess runner ──────────────────────────────────────────────────────


def run(
    cmd: str,
    dry_run: bool = False,
    check: bool = True,
    capture: bool = False,
    cwd: str | None = None,
) -> subprocess.CompletedProcess[str]:
    """Run a shell command. In dry-run mode, just print it."""
    if dry_run:
        log(f"[DRY RUN] {cmd}")
        return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
    result = subprocess.run(
        cmd, shell=True, check=False, capture_output=capture, text=True, cwd=cwd,
    )
    if result.returncode != 0:
        output = (result.stderr or "") + (result.stdout or "")
        if "rate limit" in output.lower() or "API rate limit exceeded" in output:
            raise RateLimitExceeded(
                f"GitHub API rate limit exceeded. Command: {cmd}\n{output.strip()}"
            )
        if check:
            stderr = result.stderr if capture else ""
            fatal(f"Command failed (exit {result.returncode}): {cmd}\n{stderr}")
    return result


# ─── GitHub CLI wrapper ─────────────────────────────────────────────────────


class GitHub:
    """Wraps gh CLI calls for a specific repo with dry-run support."""

    def __init__(self, cfg: Config, repo_override: str | None = None) -> None:
        self.repo = repo_override or cfg.full_repo
        self.dry_run = cfg.dry_run
        self._cfg = cfg

    def for_repo(self, full_repo: str) -> "GitHub":
        """Create a new GitHub instance targeting a different repo."""
        return GitHub(self._cfg, repo_override=full_repo)

    def run(self, cmd: str) -> subprocess.CompletedProcess[str]:
        """Run a gh command with --repo automatically appended."""
        return run(f"gh {cmd} --repo {self.repo}", dry_run=self.dry_run)

    def create_repo(self) -> None:
        result = run(f"gh repo view {self.repo} --json name", check=False, capture=True)
        if result.returncode == 0:
            warn(f"Repository {self.repo} already exists -- skipping creation")
            return
        run(f"gh repo create {self.repo} --public --add-readme --license mit")

    def create_environment(self, name: str) -> None:
        run(f"gh api repos/{self.repo}/environments/{name} -X PUT", dry_run=self.dry_run)

    def secret_set(self, name: str, value: str) -> None:
        """Set a secret. Values are always masked in dry-run output."""
        if self.dry_run:
            log(f"[DRY RUN] gh secret set {name} --body *** --repo {self.repo}")
        else:
            run(f'gh secret set {name} --body "{value}" --repo {self.repo}')

    def variable_set(self, name: str, value: str, env: str | None = None) -> None:
        """Set a variable, optionally scoped to an environment."""
        env_flag = f" --env {env}" if env else ""
        if self.dry_run:
            log(f'[DRY RUN] gh variable set {name} --body "{value}"{env_flag} --repo {self.repo}')
        else:
            run(f'gh variable set {name} --body "{value}"{env_flag} --repo {self.repo}')

    def clone(self, dest: str) -> None:
        run(f'gh repo clone {self.repo} "{dest}"')

    def workflow_run(self, workflow: str, fields: dict[str, str] | None = None) -> None:
        field_args = " ".join(f'-f {k}="{v}"' for k, v in fields.items()) if fields else ""
        self.run(f"workflow run {workflow} {field_args}".strip())

    def get_latest_run_id(self) -> str:
        """Get the database ID of the most recent workflow run."""
        result = run(
            f"gh run list --repo {self.repo} --limit 1 --json databaseId --jq .[0].databaseId",
            check=False, capture=True,
        )
        return result.stdout.strip()

    def run_watch(self) -> subprocess.CompletedProcess[str]:
        # Get the latest run ID (gh run watch requires explicit ID in non-interactive mode)
        result = run(
            f"gh run list --repo {self.repo} --limit 1 --json databaseId --jq .[0].databaseId",
            check=False, capture=True,
        )
        run_id = result.stdout.strip()
        if not run_id:
            return subprocess.CompletedProcess("gh run list", 1, stdout="", stderr="No runs found")
        return run(f"gh run watch {run_id} --repo {self.repo} --exit-status", check=False)

    def delete(self) -> None:
        run(f"gh repo delete {self.repo} --yes", check=False)


# ─── SonarCloud client ──────────────────────────────────────────────────────


class SonarCloud:
    """Wraps SonarCloud API calls for a specific org."""

    def __init__(self, token: str, org: str) -> None:
        self.token = token
        self.org = org

    def _api(self, method: str, endpoint: str, data: dict[str, str] | None = None) -> dict:
        url = f"https://sonarcloud.io/api{endpoint}"
        body = "&".join(f"{k}={v}" for k, v in data.items()).encode() if method == "POST" and data else None
        req = urllib.request.Request(url, data=body, method=method)
        creds = base64.b64encode(f"{self.token}:".encode()).decode()
        req.add_header("Authorization", f"Basic {creds}")
        if body:
            req.add_header("Content-Type", "application/x-www-form-urlencoded")
        try:
            with urllib.request.urlopen(req) as resp:
                raw = resp.read().decode()
                return json.loads(raw) if raw.strip() else {}
        except urllib.error.HTTPError as e:
            body_text = e.read().decode() if e.fp else ""
            return {"error": True, "status": e.code, "message": body_text}

    def _is_already_exists(self, result: dict) -> bool:
        return result.get("error", False) and "already exist" in result.get("message", "").lower()

    def create_org(self) -> None:
        result = self._api("POST", "/organizations/create", {"key": self.org, "name": self.org})
        if result.get("error") and not self._is_already_exists(result):
            warn(f"SonarCloud org creation: {result.get('message', 'unknown error')}")
        else:
            ok(f"SonarCloud org: {self.org}")

    def create_project(self, key: str) -> None:
        result = self._api("POST", "/projects/create", {
            "organization": self.org, "project": key, "name": key,
        })
        if result.get("error") and not self._is_already_exists(result):
            warn(f"SonarCloud project {key}: {result.get('message', 'unknown error')}")
        else:
            ok(f"SonarCloud project: {key}")

        # Rename default branch master -> main
        result = self._api("POST", "/project_branches/rename", {"project": key, "name": "main"})
        if result.get("error") and "already exists" not in result.get("message", "").lower():
            warn(f"SonarCloud branch rename for {key}: {result.get('message', 'unknown error')}")

    def delete_project(self, key: str) -> None:
        self._api("POST", "/projects/delete", {"project": key})
        ok(f"Deleted SonarCloud project: {key}")
