"""Steps 5-6: Replace repository references and language namespaces."""

from __future__ import annotations

import os

from ..config import Config
from ..files import (
    is_git_dir,
    rename_dotnet_files,
    rename_java_dirs,
    replace_in_dockerfiles,
    replace_in_file,
    replace_in_tree,
)
from ..log import fatal, log, ok
from ..templates import fixup_multirepo_docker_compose

# All text file extensions to process
TEXT_EXTS = [
    ".yml", ".yaml", ".md", ".gradle", ".gradle.kts",
    ".csproj", ".sln", ".slnx", ".cshtml", ".json",
    ".cs", ".java", ".ts", ".tsx", ".js", ".jsx",
    ".xml", ".properties", ".cfg", ".txt",
]


# ─── Step 5: Replace repository references ──────────────────────────────────


def replace_repo_references(cfg: Config, **_: object) -> None:
    log("Step 5: Replacing repository references...")

    if cfg.dry_run:
        log(f"[DRY RUN] Would replace optivem/starter -> {cfg.full_repo}")
        return

    if cfg.arch == "monolith":
        _replace_refs_in_repo(cfg.repo_dir, cfg.full_repo, cfg.owner_lower)
    else:
        _replace_refs_in_repo(cfg.repo_dir, cfg.full_repo, cfg.owner_lower)
        _replace_refs_in_repo(cfg.backend_repo_dir, cfg.backend_full_repo, cfg.owner_lower)
        _replace_refs_in_repo(cfg.frontend_repo_dir, cfg.frontend_full_repo, cfg.owner_lower)

        # Fix docker-compose image URLs: repo -> repo-frontend / repo-backend
        # Must run after replace_refs because docker-compose originally has
        # optivem/starter which gets replaced to owner/repo above.
        fixup_multirepo_docker_compose(
            cfg.repo_dir, cfg.repo, cfg.frontend_repo, cfg.backend_repo, cfg.backend_lang,
        )

    ok("Repository reference replacement complete")


def _replace_refs_in_repo(repo_dir: str, full_repo: str, owner_lower: str) -> None:
    """Replace optivem/starter references in a single repo."""
    # Pass 1: optivem/starter -> owner/repo
    n = replace_in_tree(repo_dir, "optivem/starter", full_repo, TEXT_EXTS)
    n += replace_in_dockerfiles(repo_dir, "optivem/starter", full_repo)
    ok(f"Pass 1: replaced optivem/starter -> {full_repo} ({n} files)")

    # Pass 2: optivem_starter -> owner_repo (SonarCloud underscore variant)
    underscore_new = full_repo.replace("/", "_")
    n = replace_in_tree(repo_dir, "optivem_starter", underscore_new, TEXT_EXTS)
    ok(f"Pass 2: replaced optivem_starter -> {underscore_new} ({n} files)")

    # Pass 3: SonarCloud org (scoped patterns to avoid touching optivem/actions)
    sonar_replacements = [
        ("'sonar.organization', 'optivem'", f"'sonar.organization', '{owner_lower}'"),
        ('/o:"optivem"', f'/o:"{owner_lower}"'),
        ("-Dsonar.organization=optivem", f"-Dsonar.organization={owner_lower}"),
    ]
    for old, new in sonar_replacements:
        n = replace_in_tree(repo_dir, old, new)
        if n > 0:
            ok(f"Pass 3: replaced sonar org pattern ({n} files)")

    # Safety check: optivem/actions must still be intact
    wf_dir = os.path.join(repo_dir, ".github", "workflows")
    if os.path.isdir(wf_dir):
        actions_found = any(
            "optivem/actions" in open(os.path.join(wf_dir, f), encoding="utf-8").read()
            for f in os.listdir(wf_dir)
            if f.endswith(".yml")
        )
        if not actions_found:
            fatal(f"Safety check failed: optivem/actions references were corrupted in {repo_dir}!")
        ok(f"Safety check passed: optivem/actions references intact in {repo_dir}")

    _lowercase_docker_compose_images(repo_dir)


def _lowercase_docker_compose_images(repo_dir: str) -> None:
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if is_git_dir(dirpath):
            continue
        for fname in filenames:
            if "docker-compose" not in fname or not fname.endswith(".yml"):
                continue
            filepath = os.path.join(dirpath, fname)
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    lines = f.readlines()
                changed = False
                for i, line in enumerate(lines):
                    if "image:" in line and "ghcr.io" in line:
                        prefix, _, rest = line.partition("image:")
                        lowered = f"{prefix}image:{rest.lower()}"
                        if lowered != lines[i]:
                            lines[i] = lowered
                            changed = True
                if changed:
                    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
                        f.writelines(lines)
            except (OSError, UnicodeDecodeError):
                pass
    ok("Docker-compose image URLs lowercased")


# ─── Step 6: Replace namespaces ─────────────────────────────────────────────


def replace_namespaces(cfg: Config, **_: object) -> None:
    log("Step 6: Replacing namespaces...")

    if cfg.dry_run:
        log("[DRY RUN] Would replace language-specific namespaces")
        return

    if cfg.arch == "monolith":
        assert cfg.lang is not None
        _ns_for_lang(cfg, cfg.lang, "monolith", cfg.repo_dir)
        _ns_for_lang(cfg, cfg.test_lang, "systemtest", cfg.repo_dir)
    else:
        assert cfg.backend_lang is not None
        # System repo: only system-test namespaces
        _ns_for_lang(cfg, cfg.test_lang, "systemtest", cfg.repo_dir)
        # Backend repo: backend namespaces
        _ns_for_lang(cfg, cfg.backend_lang, "backend", cfg.backend_repo_dir)
        # Frontend repo: no Java/dotnet namespaces, but handle package.json
        if cfg.frontend_lang == "react":
            _fixup_frontend_package_json(cfg)

    ok("Namespace replacement complete")


def _fixup_frontend_package_json(cfg: Config) -> None:
    """Update package.json in the frontend repo root."""
    pkg_path = os.path.join(cfg.frontend_repo_dir, "package.json")
    if os.path.isfile(pkg_path):
        replace_in_file(pkg_path, '"name": "starter-frontend"', f'"name": "{cfg.repo}-frontend"')
        replace_in_file(pkg_path, '"name": "frontend-react"', f'"name": "{cfg.repo}-frontend"')


def _ns_for_lang(cfg: Config, lang: str, component: str, repo_dir: str) -> None:
    if lang == "java":
        _ns_java(cfg, component, repo_dir)
    elif lang == "dotnet":
        _ns_dotnet(cfg, component, repo_dir)
    elif lang == "typescript":
        _ns_typescript(cfg, component, repo_dir)


def _ns_java(cfg: Config, component: str, repo_dir: str) -> None:
    old_full = f"{cfg.java_ns_old}.{component}"
    new_full = f"{cfg.java_ns_new}.{component}"

    n = replace_in_tree(repo_dir, old_full, new_full, [".java", ".gradle", ".gradle.kts", ".xml", ".properties"])
    n += replace_in_tree(repo_dir, old_full, new_full, [".yml"])
    ok(f"Java: replaced {old_full} -> {new_full} ({n} files)")

    old_dir_parts = ["com", "optivem", "starter"]
    new_dir_parts = ["com", cfg.owner_lower, cfg.repo_nohyphens]
    for dirpath, _dirnames, _filenames in os.walk(repo_dir):
        if is_git_dir(dirpath):
            continue
        if os.path.isdir(os.path.join(dirpath, *old_dir_parts)):
            rename_java_dirs(dirpath, old_dir_parts, new_dir_parts)
    ok(f"Java: renamed directories com/optivem/starter -> com/{cfg.owner_lower}/{cfg.repo_nohyphens}")


def _ns_dotnet(cfg: Config, component: str, repo_dir: str) -> None:
    component_map = {"monolith": "Monolith", "backend": "Backend", "systemtest": "SystemTest"}
    old_full = f"{cfg.dotnet_ns_old}.{component_map[component]}"
    new_full = f"{cfg.dotnet_ns_new}.{component_map[component]}"

    n = replace_in_tree(repo_dir, old_full, new_full, [".cs", ".cshtml", ".csproj", ".sln", ".slnx", ".json", ".yml"])
    n += replace_in_dockerfiles(repo_dir, old_full, new_full)
    ok(f".NET: replaced {old_full} -> {new_full} ({n} files)")

    rename_dotnet_files(repo_dir, old_full, new_full)
    ok(f".NET: renamed files {old_full}.* -> {new_full}.*")


def _ns_typescript(cfg: Config, component: str, repo_dir: str) -> None:
    if component != "systemtest":
        return

    n = replace_in_tree(repo_dir, cfg.ts_pkg_old, cfg.ts_pkg_new, [".json"])
    ok(f"TypeScript: replaced {cfg.ts_pkg_old} -> {cfg.ts_pkg_new} ({n} files)")

    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if "system-test" in dirpath and "package.json" in filenames:
            pkg_path = os.path.join(dirpath, "package.json")
            replace_in_file(pkg_path, '"author": "Optivem"', f'"author": "{cfg.owner}"')
            replace_in_file(pkg_path, '"Starter - System Tests"', f'"{cfg.system_name} - System Tests"')
            replace_in_file(pkg_path, '"optivem"', f'"{cfg.owner_lower}"')
            ok("TypeScript: updated package.json metadata")
            break

    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if is_git_dir(dirpath) or "system-test" in dirpath or "node_modules" in dirpath:
            continue
        if "package.json" in filenames:
            pkg_path = os.path.join(dirpath, "package.json")
            if "monolith" in dirpath:
                replace_in_file(pkg_path, '"name": "starter-monolith"', f'"name": "{cfg.repo}-monolith"')
            elif "backend" in dirpath:
                replace_in_file(pkg_path, '"name": "starter-backend"', f'"name": "{cfg.repo}-backend"')
