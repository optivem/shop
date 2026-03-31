"""Steps 1-3: Create repo, environments, secrets and variables."""

from __future__ import annotations

import time

from ..config import Config
from ..log import log, ok
from ..shell import GitHub


def create_repos(cfg: Config, github: GitHub, **_: object) -> None:
    log(f"Step 1: Creating repository {cfg.full_repo}...")

    if cfg.dry_run:
        log(f"[DRY RUN] gh repo create {cfg.full_repo} --public --add-readme --license mit")
        if cfg.arch == "multitier":
            log(f"[DRY RUN] gh repo create {cfg.frontend_full_repo} --public --add-readme --license mit")
            log(f"[DRY RUN] gh repo create {cfg.backend_full_repo} --public --add-readme --license mit")
        return

    github.create_repo()
    time.sleep(3)
    ok(f"Created repository: {cfg.full_repo}")

    if cfg.arch == "multitier":
        gh_frontend = github.for_repo(cfg.frontend_full_repo)
        gh_backend = github.for_repo(cfg.backend_full_repo)
        gh_frontend.create_repo()
        time.sleep(3)
        ok(f"Created repository: {cfg.frontend_full_repo}")
        gh_backend.create_repo()
        time.sleep(3)
        ok(f"Created repository: {cfg.backend_full_repo}")


def setup_environments(cfg: Config, github: GitHub, **_: object) -> None:
    log("Step 2: Creating environments...")
    prefix = f"{cfg.arch}-{cfg.lang or cfg.backend_lang}"
    for stage in ["acceptance", "qa", "production"]:
        env_name = f"{prefix}-{stage}"
        github.create_environment(env_name)
    ok(f"Created environments: {prefix}-acceptance, {prefix}-qa, {prefix}-production")


def _get_system_urls(cfg: Config) -> dict[str, str]:
    """Return system URL variables based on architecture and language."""
    lang_code = {"java": "1", "dotnet": "2", "typescript": "3"}
    lang_digit = lang_code.get(cfg.lang or cfg.backend_lang or "", "1")

    if cfg.arch == "multitier":
        return {
            "SYSTEM_UI_URL": f"http://localhost:3{lang_digit}01",
            "SYSTEM_API_URL": f"http://localhost:8{lang_digit}01",
            "ERP_URL": f"http://localhost:9{lang_digit}01/erp",
            "CLOCK_URL": f"http://localhost:9{lang_digit}01/clock",
        }
    else:
        return {
            "SYSTEM_URL": f"http://localhost:2{lang_digit}01",
        }


def setup_secrets_and_variables(cfg: Config, github: GitHub, **_: object) -> None:
    log("Step 3: Setting secrets and variables...")

    github.secret_set("DOCKERHUB_TOKEN", cfg.dockerhub_token)
    github.secret_set("SONAR_TOKEN", cfg.sonar_token)
    github.variable_set("DOCKERHUB_USERNAME", cfg.dockerhub_username)

    prefix = f"{cfg.arch}-{cfg.lang or cfg.backend_lang}"
    system_urls = _get_system_urls(cfg)
    for stage in ["acceptance", "qa", "production"]:
        env_name = f"{prefix}-{stage}"
        for var_name, var_value in system_urls.items():
            github.variable_set(var_name, var_value, env=env_name)

    if cfg.arch == "multitier":
        github.secret_set("GHCR_TOKEN", cfg.ghcr_token)

        for full_repo in [cfg.frontend_full_repo, cfg.backend_full_repo]:
            gh = github.for_repo(full_repo)
            gh.secret_set("DOCKERHUB_TOKEN", cfg.dockerhub_token)
            gh.secret_set("SONAR_TOKEN", cfg.sonar_token)
            gh.variable_set("DOCKERHUB_USERNAME", cfg.dockerhub_username)
        ok("Set secrets and variables on component repositories")

    ok("Set secrets and variables")
