#!/usr/bin/env python3
"""
Deterministic scaffold script for creating pipeline projects from starter templates.

Usage:
  Monolith:
    python scaffold.py --owner acme --system-name "Page Turner" --repo page-turner \
        --arch monolith --lang java

  Multitier:
    python scaffold.py --owner acme --system-name "Page Turner" --repo page-turner \
        --arch multitier --backend-lang java --frontend-lang react

  Test mode:
    python scaffold.py ... --test --cleanup

  Dry run:
    python scaffold.py ... --dry-run
"""

from typing import Callable

from scaffold.config import Config, parse_args, validate
from scaffold.log import fail, log, ok
from scaffold.shell import GitHub, SonarCloud
from scaffold.steps import (
    cleanup,
    clone_and_apply_template,
    commit_and_push,
    create_repo,
    create_sonarcloud_projects,
    replace_namespaces,
    replace_repo_references,
    setup_environments,
    setup_secrets_and_variables,
    update_readme,
    verify_acceptance_stage,
    verify_commit_stage,
)

StepFn = Callable[..., None]

STEPS: list[tuple[str, StepFn]] = [
    ("Create repository", create_repo),
    ("Setup environments", setup_environments),
    ("Setup secrets and variables", setup_secrets_and_variables),
    ("Clone and apply template", clone_and_apply_template),
    ("Replace repository references", replace_repo_references),
    ("Replace namespaces", replace_namespaces),
    ("Update README", update_readme),
    ("Create SonarCloud projects", create_sonarcloud_projects),
    ("Commit and push", commit_and_push),
    ("Verify commit stage", verify_commit_stage),
    ("Verify acceptance stage", verify_acceptance_stage),
]


def print_banner(cfg: Config) -> None:
    print()
    print("=" * 42)
    print("  Pipeline Project Setup")
    print("=" * 42)
    print()
    log(f"Owner:       {cfg.owner}")
    log(f"Repo:        {cfg.repo}")
    log(f"System:      {cfg.system_name}")
    log(f"Arch:        {cfg.arch}")
    if cfg.arch == "monolith":
        log(f"Language:    {cfg.lang}")
    else:
        log(f"Backend:     {cfg.backend_lang}")
        log(f"Frontend:    {cfg.frontend_lang}")
        log(f"Backend repo: {cfg.backend_full_repo}")
        log(f"Frontend repo: {cfg.frontend_full_repo}")
    log(f"Test lang:   {cfg.test_lang}")
    log(f"Dry run:     {cfg.dry_run}")
    log(f"Test mode:   {cfg.test_mode}")
    log(f"Workdir:     {cfg.workdir}")
    print()


def main() -> None:
    args = parse_args()
    cfg = validate(args)

    github = GitHub(cfg)
    sonarcloud = SonarCloud(cfg.sonar_token, cfg.owner_lower)

    print_banner(cfg)

    errors = 0
    for step_name, step_func in STEPS:
        try:
            step_func(cfg=cfg, github=github, sonarcloud=sonarcloud)
        except SystemExit:
            errors += 1
            break
        except Exception as e:
            fail(f"Step failed: {step_name} -- {e}")
            errors += 1
            break

    print()
    print("=" * 42)
    if errors > 0:
        fail(f"Setup completed with {errors} error(s)")
    else:
        ok("All steps passed!")
    print()
    print(f"  Repository: https://github.com/{cfg.full_repo}")
    print(f"  Actions:    https://github.com/{cfg.full_repo}/actions")
    if cfg.arch == "multitier":
        print(f"  Backend:    https://github.com/{cfg.backend_full_repo}")
        print(f"  Frontend:   https://github.com/{cfg.frontend_full_repo}")
    print()

    cleanup(cfg, github, sonarcloud)
    raise SystemExit(errors)


if __name__ == "__main__":
    main()
