"""Step 4: Clone repo(s) and apply template files."""

from __future__ import annotations

import os
import shutil

from ..config import Config
from ..files import replace_in_file
from ..log import log, ok
from ..shell import GitHub
from ..templates import (
    copy_version,
    copy_workflows,
    fixup_commit_stage_for_standalone,
    fixup_multirepo_image_urls,
    fixup_multirepo_token,
    select_docker_compose,
)

# Internal port exposed by each language's Docker image
_INTERNAL_PORTS = {"java": 8080, "dotnet": 8080, "typescript": 3000}


def clone_repos(cfg: Config, github: GitHub, **_: object) -> None:
    log("Step 4: Cloning repo(s)...")

    if cfg.dry_run:
        log("[DRY RUN] Would clone repo(s)")
        return

    repo_dir = os.path.join(cfg.workdir, "repo")
    github.clone(repo_dir)
    cfg.repo_dir = repo_dir
    ok(f"Cloned {cfg.full_repo}")

    if cfg.arch == "multitier":
        frontend_dir = os.path.join(cfg.workdir, "repo-frontend")
        backend_dir = os.path.join(cfg.workdir, "repo-backend")

        github.for_repo(cfg.frontend_full_repo).clone(frontend_dir)
        cfg.frontend_repo_dir = frontend_dir
        ok(f"Cloned {cfg.frontend_full_repo}")

        github.for_repo(cfg.backend_full_repo).clone(backend_dir)
        cfg.backend_repo_dir = backend_dir
        ok(f"Cloned {cfg.backend_full_repo}")


def apply_template(cfg: Config, **_: object) -> None:
    log("Step 5: Applying template files...")

    if cfg.dry_run:
        log("[DRY RUN] Would apply template files")
        return

    repo_dir = cfg.repo_dir
    starter = cfg.starter_path

    os.makedirs(os.path.join(repo_dir, ".github", "workflows"), exist_ok=True)

    if cfg.arch == "monolith":
        _apply_monolith(cfg, repo_dir, starter)
    else:
        _apply_multitier(cfg, repo_dir, starter)

    ok("Applied template files")


def _apply_monolith(cfg: Config, repo_dir: str, starter: str) -> None:
    lang = cfg.lang
    test_lang = cfg.test_lang
    assert lang is not None

    workflows = [
        f"monolith-{lang}-commit-stage.yml",
        f"monolith-{test_lang}-acceptance-stage.yml",
        f"monolith-{test_lang}-qa-stage.yml",
        f"monolith-{test_lang}-qa-signoff.yml",
        f"monolith-{test_lang}-prod-stage.yml",
    ]
    if lang == test_lang:
        workflows.append(f"monolith-{lang}-verify.yml")
    copy_workflows(workflows, starter, repo_dir)

    shutil.copytree(
        os.path.join(starter, "system", "monolith", lang),
        os.path.join(repo_dir, "system", "monolith", lang),
    )

    test_dst = os.path.join(repo_dir, "system-test", test_lang)
    shutil.copytree(os.path.join(starter, "system-test", test_lang), test_dst)
    select_docker_compose(test_dst, "single")
    copy_version(starter, repo_dir)

    if lang != test_lang:
        _fixup_monolith_cross_lang(repo_dir, lang, test_lang)


def _apply_multitier(cfg: Config, repo_dir: str, starter: str) -> None:
    backend_lang = cfg.backend_lang
    frontend_lang = cfg.frontend_lang
    test_lang = cfg.test_lang
    assert backend_lang is not None and frontend_lang is not None

    frontend_dir = cfg.frontend_repo_dir
    backend_dir = cfg.backend_repo_dir

    # ── System repo: workflows, system-test, VERSION ───────────────────────
    system_workflows = [
        f"multitier-system-{test_lang}-acceptance-stage.yml",
        f"multitier-system-{test_lang}-qa-stage.yml",
        f"multitier-system-{test_lang}-qa-signoff.yml",
        f"multitier-system-{test_lang}-prod-stage.yml",
    ]
    if backend_lang == test_lang:
        system_workflows.append(f"multitier-{backend_lang}-verify.yml")
    copy_workflows(system_workflows, starter, repo_dir)

    test_dst = os.path.join(repo_dir, "system-test", test_lang)
    shutil.copytree(os.path.join(starter, "system-test", test_lang), test_dst)
    select_docker_compose(test_dst, "multi")
    copy_version(starter, repo_dir)

    # Cross-language fixup must run BEFORE image URL fixup, because the
    # template uses the test_lang in image names (e.g. multitier-backend-dotnet)
    # and the cross-lang fixup renames them to the actual backend_lang.
    if backend_lang != test_lang:
        _fixup_multitier_cross_lang_system(repo_dir, backend_lang, test_lang)

    # Fix system workflows for multi-repo: image URLs and token
    fixup_multirepo_image_urls(
        repo_dir, cfg.repo, cfg.frontend_repo, cfg.backend_repo, backend_lang,
    )
    fixup_multirepo_token(repo_dir)

    ok("Applied system repo template")

    # ── Backend repo: code at root, commit stage workflow ──────────────────
    backend_component = f"backend-{backend_lang}"
    backend_src = os.path.join(starter, "system", "multitier", backend_component)

    os.makedirs(os.path.join(backend_dir, ".github", "workflows"), exist_ok=True)

    # Copy component code to repo root
    for item in os.listdir(backend_src):
        src = os.path.join(backend_src, item)
        dst = os.path.join(backend_dir, item)
        if os.path.isdir(src):
            shutil.copytree(src, dst)
        else:
            shutil.copy2(src, dst)

    # Copy commit stage workflow
    copy_workflows(
        [f"multitier-{backend_component}-commit-stage.yml"],
        starter, backend_dir,
    )
    fixup_commit_stage_for_standalone(backend_dir, backend_component, backend_lang)
    ok("Applied backend repo template")

    # ── Frontend repo: code at root, commit stage workflow ─────────────────
    frontend_component = f"frontend-{frontend_lang}"
    frontend_src = os.path.join(starter, "system", "multitier", frontend_component)

    os.makedirs(os.path.join(frontend_dir, ".github", "workflows"), exist_ok=True)

    for item in os.listdir(frontend_src):
        src = os.path.join(frontend_src, item)
        dst = os.path.join(frontend_dir, item)
        if os.path.isdir(src):
            shutil.copytree(src, dst)
        else:
            shutil.copy2(src, dst)

    copy_workflows(
        [f"multitier-{frontend_component}-commit-stage.yml"],
        starter, frontend_dir,
    )
    fixup_commit_stage_for_standalone(frontend_dir, frontend_component, frontend_lang)
    ok("Applied frontend repo template")


# ─── Cross-language fixups ─────────────────────────────────────────────────


def _fixup_monolith_cross_lang(
    repo_dir: str, lang: str, test_lang: str,
) -> None:
    """Fix Docker image name and port when system language != test language."""
    old_image = f"monolith-{test_lang}-monolith"
    new_image = f"monolith-{lang}-monolith"

    targets = [
        os.path.join(repo_dir, ".github", "workflows", f"monolith-{test_lang}-acceptance-stage.yml"),
        os.path.join(repo_dir, ".github", "workflows", f"monolith-{test_lang}-qa-stage.yml"),
        os.path.join(repo_dir, ".github", "workflows", f"monolith-{test_lang}-prod-stage.yml"),
        os.path.join(repo_dir, "system-test", test_lang, "docker-compose.multitier.yml"),
    ]
    for path in targets:
        if os.path.isfile(path):
            replace_in_file(path, old_image, new_image)

    # Fix port mapping: template uses test_lang's port, but we need lang's port
    system_port = _INTERNAL_PORTS[lang]
    template_port = _INTERNAL_PORTS[test_lang]
    if system_port != template_port:
        compose = os.path.join(repo_dir, "system-test", test_lang, "docker-compose.multitier.yml")
        if os.path.isfile(compose):
            replace_in_file(compose, f"8080:{template_port}", f"8080:{system_port}")

    ok(f"Cross-language fixup: {old_image} -> {new_image}")


def _fixup_multitier_cross_lang_system(
    repo_dir: str, backend_lang: str, test_lang: str,
) -> None:
    """Fix Docker backend image name in system repo when backend language != test language."""
    old_image = f"multitier-backend-{test_lang}"
    new_image = f"multitier-backend-{backend_lang}"

    targets = [
        os.path.join(repo_dir, ".github", "workflows", f"multitier-system-{test_lang}-acceptance-stage.yml"),
        os.path.join(repo_dir, ".github", "workflows", f"multitier-system-{test_lang}-qa-stage.yml"),
        os.path.join(repo_dir, ".github", "workflows", f"multitier-system-{test_lang}-prod-stage.yml"),
        os.path.join(repo_dir, "system-test", test_lang, "docker-compose.monolith.yml"),
    ]
    for path in targets:
        if os.path.isfile(path):
            replace_in_file(path, old_image, new_image)

    ok(f"Cross-language fixup: {old_image} -> {new_image}")
