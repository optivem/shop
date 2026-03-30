"""Shell and API helpers: GitHub CLI wrapper, SonarCloud client, subprocess runner."""

from __future__ import annotations

import base64
import json
import subprocess
import urllib.error
import urllib.request

from .config import Config
from .log import log, fatal, ok, warn


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
    if check and result.returncode != 0:
        stderr = result.stderr if capture else ""
        fatal(f"Command failed (exit {result.returncode}): {cmd}\n{stderr}")
    return result


# ─── GitHub CLI wrapper ─────────────────────────────────────────────────────


class GitHub:
    """Wraps gh CLI calls for a specific repo with dry-run support."""

    def __init__(self, cfg: Config) -> None:
        self.repo = cfg.full_repo
        self.dry_run = cfg.dry_run

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

    def workflow_run(self, workflow: str) -> None:
        self.run(f"workflow run {workflow}")

    def run_watch(self) -> subprocess.CompletedProcess[str]:
        return run(f"gh run watch --repo {self.repo} --exit-status", check=False, capture=False)

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
