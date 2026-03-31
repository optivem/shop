#!/usr/bin/env python3
"""
Cleanup script — deletes GitHub repos and SonarCloud projects created by verification.

Reads verifier-config.json defaults to determine the owner and prefix, then calls
delete-repos.sh and delete-sonar-projects.sh from github-utils.

Usage:
  Dry run (preview what would be deleted):
    python cleanup.py --dry-run

  Delete repos and sonar projects:
    python cleanup.py

  Delete only repos:
    python cleanup.py --repos-only

  Delete only sonar projects:
    python cleanup.py --sonar-only

  Custom config path:
    python cleanup.py --config path/to/verifier-config.json
"""

import argparse
import os
import subprocess
import sys

from verify.config import find_config_path, load_config
from scaffold.log import fail, log, ok


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Clean up GitHub repos and SonarCloud projects created by verification.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--dry-run", action="store_true", help="Preview what would be deleted without actually deleting")
    parser.add_argument("--repos-only", action="store_true", help="Only delete GitHub repos")
    parser.add_argument("--sonar-only", action="store_true", help="Only delete SonarCloud projects")
    parser.add_argument("--config", help="Path to verifier-config.json (default: auto-detect)")
    return parser.parse_args()


def find_github_utils_dir() -> str:
    """Locate the github-utils/scripts directory relative to the starter repo."""
    starter_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    academy_root = os.path.dirname(starter_root)
    scripts_dir = os.path.join(academy_root, "github-utils", "scripts")
    if os.path.isdir(scripts_dir):
        return scripts_dir
    raise FileNotFoundError(f"Cannot find github-utils/scripts (tried {scripts_dir})")


def delete_repos(owner: str, prefix: str, dry_run: bool, scripts_dir: str) -> bool:
    """Call delete-repos.sh to remove GitHub repos matching the prefix."""
    script = os.path.join(scripts_dir, "delete-repos.sh")
    cmd = ["bash", script, owner, "--prefix", prefix]
    env = {**os.environ, **({"DRY_RUN": "1"} if dry_run else {})}

    log(f"Deleting GitHub repos: owner={owner}, prefix={prefix}")
    result = subprocess.run(cmd, env=env)
    return result.returncode == 0


def delete_sonar_projects(owner: str, prefix: str, dry_run: bool, scripts_dir: str) -> bool:
    """Call delete-sonar-projects.sh to remove SonarCloud projects matching the prefix."""
    script = os.path.join(scripts_dir, "delete-sonar-projects.sh")
    sonar_prefix = f"{owner}_{prefix}"
    cmd = ["bash", script, owner, "--prefix", sonar_prefix]
    env = {**os.environ, **({"DRY_RUN": "1"} if dry_run else {})}

    log(f"Deleting SonarCloud projects: org={owner}, prefix={sonar_prefix}")
    result = subprocess.run(cmd, env=env)
    return result.returncode == 0


def main() -> None:
    args = parse_args()
    config_path = find_config_path(args.config)
    config = load_config(config_path)

    defaults = config["defaults"]
    owner = defaults["GITHUB_OWNER"]
    system_name = defaults["SYSTEM_NAME"]
    prefix = system_name.lower().replace(" ", "-") + "-"

    log(f"Config: {config_path}")
    log(f"Owner: {owner}, Prefix: {prefix}")

    if args.dry_run:
        log("Dry run — no resources will be deleted")

    success = True

    if not args.sonar_only:
        scripts_dir = find_github_utils_dir()
        if not delete_repos(owner, prefix, args.dry_run, scripts_dir):
            fail("GitHub repo deletion failed")
            success = False

    if not args.repos_only:
        scripts_dir = find_github_utils_dir()
        if not delete_sonar_projects(owner, prefix, args.dry_run, scripts_dir):
            fail("SonarCloud project deletion failed")
            success = False

    print()
    if success:
        ok("Cleanup completed successfully!")
    else:
        fail("Cleanup completed with errors.")

    raise SystemExit(0 if success else 1)


if __name__ == "__main__":
    main()
