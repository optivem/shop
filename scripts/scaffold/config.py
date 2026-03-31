"""Configuration: CLI parsing, validation, and Config dataclass."""

from __future__ import annotations

import argparse
import os
import secrets
import subprocess
import sys
import tempfile
from dataclasses import dataclass, field

from .log import fatal


def to_pascal_case(s: str) -> str:
    """'page-turner' -> 'PageTurner'"""
    return "".join(part.capitalize() for part in s.split("-"))


def to_java_lower(s: str) -> str:
    """'page-turner' -> 'pageturner'"""
    return s.replace("-", "").lower()


@dataclass
class Config:
    owner: str
    repo: str
    full_repo: str
    system_name: str
    arch: str
    lang: str | None
    backend_lang: str | None
    frontend_lang: str | None
    test_lang: str
    dry_run: bool
    workdir: str
    starter_path: str
    dockerhub_username: str
    dockerhub_token: str
    sonar_token: str
    # Derived namespaces
    owner_pascal: str
    owner_lower: str
    repo_pascal: str
    repo_nohyphens: str
    java_ns_old: str = field(default="com.optivem.starter")
    java_ns_new: str = ""
    dotnet_ns_old: str = field(default="Optivem.Starter")
    dotnet_ns_new: str = ""
    ts_pkg_old: str = field(default="@optivem/starter-system-test")
    ts_pkg_new: str = ""
    # Multi-repo (multitier only)
    ghcr_token: str = ""
    frontend_repo: str = ""
    backend_repo: str = ""
    frontend_full_repo: str = ""
    backend_full_repo: str = ""
    # Set after clone
    repo_dir: str = ""
    frontend_repo_dir: str = ""
    backend_repo_dir: str = ""


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Scaffold a pipeline project from starter templates.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--owner", required=True, help="GitHub username or org")
    parser.add_argument("--system-name", required=True, help='System name, e.g. "Page Turner"')
    parser.add_argument("--repo", required=True, help="Repository name, e.g. page-turner")
    parser.add_argument("--arch", required=True, choices=["monolith", "multitier"], help="Architecture")
    parser.add_argument("--lang", choices=["java", "dotnet", "typescript"], help="System language (monolith)")
    parser.add_argument("--test-lang", choices=["java", "dotnet", "typescript"], help="Test language (defaults to --lang or --backend-lang)")
    parser.add_argument("--backend-lang", choices=["java", "dotnet", "typescript"], help="Backend language (multitier)")
    parser.add_argument("--frontend-lang", choices=["react"], help="Frontend language (multitier)")
    parser.add_argument("--random-suffix", action="store_true", help="Append 4-char hex suffix to repo name")
    parser.add_argument("--dry-run", action="store_true", help="Print actions without executing")
    parser.add_argument("--workdir", help="Working directory for cloning (default: temp dir)")
    return parser.parse_args()


def validate(args: argparse.Namespace) -> Config:
    """Validate inputs and resolve defaults."""
    if args.arch == "monolith":
        if not args.lang:
            fatal("--lang is required for monolith architecture")
        lang: str | None = args.lang
        backend_lang: str | None = None
        frontend_lang: str | None = None
        test_lang: str = args.test_lang or args.lang
    else:
        if not args.backend_lang:
            fatal("--backend-lang is required for multitier architecture")
        if not args.frontend_lang:
            fatal("--frontend-lang is required for multitier architecture")
        lang = None
        backend_lang = args.backend_lang
        frontend_lang = args.frontend_lang
        test_lang = args.test_lang or args.backend_lang

    repo: str = args.repo
    if args.random_suffix:
        repo = f"{repo}-{secrets.token_hex(2)}"

    dockerhub_username = os.environ.get("DOCKERHUB_USERNAME", "")
    dockerhub_token = os.environ.get("DOCKERHUB_TOKEN", "")
    sonar_token = os.environ.get("SONAR_TOKEN", "")
    ghcr_token = os.environ.get("GHCR_TOKEN", "")

    required_vars = [
        ("DOCKERHUB_USERNAME", dockerhub_username),
        ("DOCKERHUB_TOKEN", dockerhub_token),
        ("SONAR_TOKEN", sonar_token),
    ]
    if args.arch == "multitier":
        required_vars.append(("GHCR_TOKEN", ghcr_token))

    if not args.dry_run:
        for name, val in required_vars:
            if not val:
                if name == "GHCR_TOKEN":
                    fatal(
                        f"{name} environment variable is required for multitier setup.\n"
                        "  Create a Personal Access Token (classic) with write:packages + read:packages scopes:\n"
                        "  https://github.com/settings/tokens\n"
                        "  Then: export GHCR_TOKEN=<your-token>"
                    )
                fatal(f"{name} environment variable is required")

    script_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    starter_path = os.path.dirname(script_dir) if os.path.basename(script_dir) == "scripts" else script_dir
    # Re-derive: scaffold package is scripts/scaffold/, so __file__ is scripts/scaffold/config.py
    # starter_path should be the parent of scripts/
    starter_path = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    if not os.path.isfile(os.path.join(starter_path, "VERSION")):
        fatal(f"Cannot find VERSION file in {starter_path} — script must be inside starter/scripts/")

    result = subprocess.run("gh auth status", shell=True, capture_output=True, text=True)
    if result.returncode != 0 and not args.dry_run:
        fatal("gh CLI is not authenticated. Run 'gh auth login' first.")

    owner: str = args.owner
    owner_pascal = to_pascal_case(owner) if "-" in owner else owner.capitalize()
    owner_lower = owner.lower()
    repo_pascal = to_pascal_case(repo)
    repo_nohyphens = to_java_lower(repo)

    frontend_repo = f"{repo}-frontend" if args.arch == "multitier" else ""
    backend_repo = f"{repo}-backend" if args.arch == "multitier" else ""

    return Config(
        owner=owner,
        repo=repo,
        full_repo=f"{owner}/{repo}",
        system_name=args.system_name,
        arch=args.arch,
        lang=lang,
        backend_lang=backend_lang,
        frontend_lang=frontend_lang,
        test_lang=test_lang,
        dry_run=args.dry_run,
        workdir=args.workdir or tempfile.mkdtemp(prefix="scaffold-"),
        starter_path=starter_path,
        dockerhub_username=dockerhub_username,
        dockerhub_token=dockerhub_token,
        sonar_token=sonar_token,
        ghcr_token=ghcr_token,
        frontend_repo=frontend_repo,
        backend_repo=backend_repo,
        frontend_full_repo=f"{owner}/{frontend_repo}" if frontend_repo else "",
        backend_full_repo=f"{owner}/{backend_repo}" if backend_repo else "",
        owner_pascal=owner_pascal,
        owner_lower=owner_lower,
        repo_pascal=repo_pascal,
        repo_nohyphens=repo_nohyphens,
        java_ns_new=f"com.{owner_lower}.{repo_nohyphens}",
        dotnet_ns_new=f"{owner_pascal}.{repo_pascal}",
        ts_pkg_new=f"@{owner_lower}/{repo}-system-test",
    )
