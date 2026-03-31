"""Steps 7-12: README, SonarCloud, commit/push, verify, cleanup."""

from __future__ import annotations

import os
import shutil
import time

from ..config import Config
from ..log import fail, fatal, log, ok
from ..shell import GitHub, SonarCloud, run


def get_sonar_project_keys(cfg: Config) -> list[str]:
    prefix = f"{cfg.owner}_{cfg.repo}"
    if cfg.arch == "monolith":
        return [f"{prefix}-monolith-{cfg.lang}"]
    else:
        return [
            f"{cfg.owner}_{cfg.backend_repo}-multitier-backend-{cfg.backend_lang}",
            f"{cfg.owner}_{cfg.frontend_repo}-multitier-frontend-{cfg.frontend_lang}",
        ]


# ─── Step 7: Update README ──────────────────────────────────────────────────


def update_readme(cfg: Config, **_: object) -> None:
    log("Step 7: Generating README...")

    if cfg.dry_run:
        log("[DRY RUN] Would generate README.md")
        return

    if cfg.arch == "monolith":
        _write_readme(cfg.repo_dir, cfg.system_name, _generate_badges(cfg), cfg.owner)
    else:
        _write_system_readme(cfg)
        _write_component_readme(
            cfg.backend_repo_dir, cfg.system_name, "Backend",
            cfg.backend_full_repo, cfg.backend_lang, "backend", cfg.owner,
        )
        _write_component_readme(
            cfg.frontend_repo_dir, cfg.system_name, "Frontend",
            cfg.frontend_full_repo, cfg.frontend_lang, "frontend", cfg.owner,
        )

    ok("Generated README.md")


def _write_readme(repo_dir: str, title: str, badges: str, owner: str) -> None:
    readme = (
        f"# {title}\n\n"
        f"{badges}\n"
        f"## License\n\nMIT License\n\n"
        f"## Contributors\n\n"
        f"- [{owner}](https://github.com/{owner})\n"
    )
    with open(os.path.join(repo_dir, "README.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(readme)


def _write_system_readme(cfg: Config) -> None:
    bl, fl, tl = cfg.backend_lang, cfg.frontend_lang, cfg.test_lang
    base = f"https://github.com/{cfg.full_repo}/actions/workflows"

    # System repo badges: commit stages point to component repos
    backend_base = f"https://github.com/{cfg.backend_full_repo}/actions/workflows"
    frontend_base = f"https://github.com/{cfg.frontend_full_repo}/actions/workflows"

    badge_items = [
        (f"{backend_base}/multitier-backend-{bl}-commit-stage.yml", "backend-commit-stage"),
        (f"{frontend_base}/multitier-frontend-{fl}-commit-stage.yml", "frontend-commit-stage"),
        (f"{base}/multitier-system-{tl}-acceptance-stage.yml", "acceptance-stage"),
        (f"{base}/multitier-system-{tl}-qa-stage.yml", "qa-stage"),
        (f"{base}/multitier-system-{tl}-qa-signoff.yml", "qa-signoff"),
        (f"{base}/multitier-system-{tl}-prod-stage.yml", "prod-stage"),
    ]
    badges = "\n".join(
        f"[![{label}]({url}/badge.svg)]({url})"
        for url, label in badge_items
    ) + "\n"

    repos_section = (
        f"## Repositories\n\n"
        f"- [{cfg.backend_repo}](https://github.com/{cfg.backend_full_repo}) — Backend ({bl})\n"
        f"- [{cfg.frontend_repo}](https://github.com/{cfg.frontend_full_repo}) — Frontend ({fl})\n"
    )

    readme = (
        f"# {cfg.system_name}\n\n"
        f"{badges}\n"
        f"{repos_section}\n"
        f"## License\n\nMIT License\n\n"
        f"## Contributors\n\n"
        f"- [{cfg.owner}](https://github.com/{cfg.owner})\n"
    )
    with open(os.path.join(cfg.repo_dir, "README.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(readme)


def _write_component_readme(
    repo_dir: str, system_name: str, component_label: str,
    full_repo: str, lang: str, component_type: str, owner: str,
) -> None:
    wf_name = f"multitier-{component_type}-{lang}-commit-stage.yml"
    base = f"https://github.com/{full_repo}/actions/workflows"
    badges = f"[![commit-stage]({base}/{wf_name}/badge.svg)]({base}/{wf_name})\n"

    readme = (
        f"# {system_name} — {component_label}\n\n"
        f"{badges}\n"
        f"## License\n\nMIT License\n\n"
        f"## Contributors\n\n"
        f"- [{owner}](https://github.com/{owner})\n"
    )
    with open(os.path.join(repo_dir, "README.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write(readme)


def _generate_badges(cfg: Config) -> str:
    base = f"https://github.com/{cfg.full_repo}/actions/workflows"

    if cfg.arch == "monolith":
        lang, test_lang = cfg.lang, cfg.test_lang
        items = [
            (f"monolith-{lang}-commit-stage.yml", "commit-stage"),
            (f"monolith-{test_lang}-acceptance-stage.yml", "acceptance-stage"),
            (f"monolith-{test_lang}-qa-stage.yml", "qa-stage"),
            (f"monolith-{test_lang}-qa-signoff.yml", "qa-signoff"),
            (f"monolith-{test_lang}-prod-stage.yml", "prod-stage"),
        ]
    else:
        bl, fl, tl = cfg.backend_lang, cfg.frontend_lang, cfg.test_lang
        items = [
            (f"multitier-backend-{bl}-commit-stage.yml", "backend-commit-stage"),
            (f"multitier-frontend-{fl}-commit-stage.yml", "frontend-commit-stage"),
            (f"multitier-system-{tl}-acceptance-stage.yml", "acceptance-stage"),
            (f"multitier-system-{tl}-qa-stage.yml", "qa-stage"),
            (f"multitier-system-{tl}-qa-signoff.yml", "qa-signoff"),
            (f"multitier-system-{tl}-prod-stage.yml", "prod-stage"),
        ]

    return "\n".join(
        f"[![{label}]({base}/{wf}/badge.svg)]({base}/{wf})"
        for wf, label in items
    ) + "\n"


# ─── Step 8: Create SonarCloud projects ─────────────────────────────────────


def create_sonarcloud_projects(cfg: Config, sonarcloud: SonarCloud, **_: object) -> None:
    log("Step 8: Creating SonarCloud projects...")

    if cfg.dry_run:
        log("[DRY RUN] Would create SonarCloud org and project(s)")
        return

    sonarcloud.create_org()
    for key in get_sonar_project_keys(cfg):
        sonarcloud.create_project(key)


# ─── Step 9: Commit and push ────────────────────────────────────────────────


def commit_and_push(cfg: Config, **_: object) -> None:
    log("Step 9: Committing and pushing...")

    if cfg.dry_run:
        log("[DRY RUN] Would git add, commit, push")
        return

    _commit_and_push_repo(cfg.repo_dir, cfg.full_repo)

    if cfg.arch == "multitier":
        _commit_and_push_repo(cfg.backend_repo_dir, cfg.backend_full_repo)
        _commit_and_push_repo(cfg.frontend_repo_dir, cfg.frontend_full_repo)


def _commit_and_push_repo(repo_dir: str, full_repo: str) -> None:
    run("git add -A", cwd=repo_dir)
    run('git commit -m "Apply pipeline template"', cwd=repo_dir)
    run("git push", cwd=repo_dir)
    ok(f"Pushed template to {full_repo}")


# ─── Step 10: Verify commit stage ───────────────────────────────────────────


def verify_commit_stage(cfg: Config, github: GitHub, **_: object) -> None:
    log("Step 10: Verifying commit stage workflow...")

    if cfg.dry_run:
        log("[DRY RUN] Would wait for commit stage workflow")
        return

    time.sleep(5)

    if cfg.arch == "monolith":
        _verify_workflow(github, "Commit stage")
    else:
        # Wait for commit stages on both component repos
        gh_backend = github.for_repo(cfg.backend_full_repo)
        gh_frontend = github.for_repo(cfg.frontend_full_repo)

        _verify_workflow(gh_backend, "Backend commit stage")
        _verify_workflow(gh_frontend, "Frontend commit stage")


# ─── Step 11: Verify acceptance stage ────────────────────────────────────────


def verify_acceptance_stage(cfg: Config, github: GitHub, **_: object) -> None:
    log("Step 11: Triggering and verifying acceptance stage...")

    if cfg.dry_run:
        log("[DRY RUN] Would trigger and wait for acceptance stage workflow")
        return

    test_lang = cfg.test_lang
    if cfg.arch == "monolith":
        wf = f"monolith-{test_lang}-acceptance-stage.yml"
    else:
        wf = f"multitier-system-{test_lang}-acceptance-stage.yml"

    _verify_workflow(github, "Acceptance stage", trigger_workflow=wf)


def _verify_workflow(github: GitHub, label: str, trigger_workflow: str | None = None) -> None:
    if trigger_workflow:
        github.workflow_run(trigger_workflow)
        time.sleep(5)

    result = github.run_watch()
    if result.returncode != 0:
        fail(f"{label} failed!")
        fatal(f"{label} workflow failed. Check: https://github.com/{github.repo}/actions")
    ok(f"{label} passed!")


# ─── Step 12: Cleanup ───────────────────────────────────────────────────────


def cleanup(cfg: Config, github: GitHub, sonarcloud: SonarCloud) -> None:
    if not cfg.test_mode:
        return

    should_cleanup = cfg.cleanup
    if should_cleanup == "ask":
        answer = input(f"\nDelete test repository {cfg.full_repo}? [y/N] ").strip().lower()
        should_cleanup = "yes" if answer in ("y", "yes") else "no"

    if should_cleanup == "yes":
        log(f"Cleaning up: deleting {cfg.full_repo}...")
        github.delete()
        ok(f"Deleted repository {cfg.full_repo}")

        if cfg.arch == "multitier":
            gh_frontend = github.for_repo(cfg.frontend_full_repo)
            gh_backend = github.for_repo(cfg.backend_full_repo)
            gh_frontend.delete()
            ok(f"Deleted repository {cfg.frontend_full_repo}")
            gh_backend.delete()
            ok(f"Deleted repository {cfg.backend_full_repo}")

        for key in get_sonar_project_keys(cfg):
            sonarcloud.delete_project(key)

        if cfg.repo_dir and os.path.exists(cfg.repo_dir):
            shutil.rmtree(cfg.repo_dir, ignore_errors=True)
        if cfg.frontend_repo_dir and os.path.exists(cfg.frontend_repo_dir):
            shutil.rmtree(cfg.frontend_repo_dir, ignore_errors=True)
        if cfg.backend_repo_dir and os.path.exists(cfg.backend_repo_dir):
            shutil.rmtree(cfg.backend_repo_dir, ignore_errors=True)
        ok("Cleanup complete")
    else:
        log(f"Keeping repository: https://github.com/{cfg.full_repo}")
        if cfg.arch == "multitier":
            log(f"Keeping repository: https://github.com/{cfg.frontend_full_repo}")
            log(f"Keeping repository: https://github.com/{cfg.backend_full_repo}")
