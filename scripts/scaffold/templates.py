"""Template helpers: copy workflows, docker-compose selection, VERSION, multi-repo fixups."""

from __future__ import annotations

import os
import shutil

from .files import replace_in_file
from .log import warn


def copy_workflows(workflow_names: list[str], starter: str, repo_dir: str) -> None:
    """Copy workflow files from starter to repo."""
    wf_src = os.path.join(starter, ".github", "workflows")
    wf_dst = os.path.join(repo_dir, ".github", "workflows")
    for wf in workflow_names:
        src = os.path.join(wf_src, wf)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(wf_dst, wf))
        else:
            warn(f"Workflow not found: {wf}")


def select_docker_compose(test_dst: str, variant: str) -> None:
    """Keep the chosen docker-compose variant, remove the other.
    variant: 'single' for monolith, 'multi' for multitier.
    The kept file retains its original name (e.g. docker-compose.multitier.yml)
    because workflows reference the variant-specific filename.
    """
    remove = "multi" if variant == "single" else "single"
    remove_path = os.path.join(test_dst, f"docker-compose.{remove}.yml")

    if os.path.exists(remove_path):
        os.remove(remove_path)


def copy_version(starter: str, repo_dir: str) -> None:
    """Copy VERSION file from starter to repo."""
    src = os.path.join(starter, "VERSION")
    if os.path.exists(src):
        shutil.copy2(src, os.path.join(repo_dir, "VERSION"))


# ─── Multi-repo fixups ────────────────────────────────────────────────────────


def fixup_multirepo_image_urls(repo_dir: str, repo_name: str,
                                frontend_repo: str, backend_repo: str,
                                backend_lang: str) -> None:
    """Replace ${{ github.event.repository.name }} with component repo names in
    acceptance, QA, and prod stage workflows and docker-compose files."""
    wf_dir = os.path.join(repo_dir, ".github", "workflows")
    if not os.path.isdir(wf_dir):
        return

    # In workflows, the image URLs use ${{ github.event.repository.name }}
    # which resolves to the system repo name. For multi-repo, each component
    # image lives under its own repo's GHCR namespace.
    old_frontend = f"${{{{ github.event.repository.name }}}}/multitier-frontend-react"
    new_frontend = f"{frontend_repo}/multitier-frontend-react"
    old_backend = f"${{{{ github.event.repository.name }}}}/multitier-backend-{backend_lang}"
    new_backend = f"{backend_repo}/multitier-backend-{backend_lang}"

    for fname in os.listdir(wf_dir):
        if not fname.endswith(".yml"):
            continue
        # Only fix system-level workflows (acceptance, qa, prod)
        if "system" not in fname:
            continue
        path = os.path.join(wf_dir, fname)
        replace_in_file(path, old_frontend, new_frontend)
        replace_in_file(path, old_backend, new_backend)



def fixup_multirepo_token(repo_dir: str) -> None:
    """Replace secrets.GITHUB_TOKEN with secrets.GHCR_TOKEN in
    promote-to-rc and tag-docker-images steps across system workflows."""
    wf_dir = os.path.join(repo_dir, ".github", "workflows")
    if not os.path.isdir(wf_dir):
        return

    # The token replacement only applies to specific steps that do cross-repo
    # docker image tagging. We target the Promote to RC and Tag Docker Images
    # steps in acceptance and prod stage workflows.
    for fname in os.listdir(wf_dir):
        if not fname.endswith(".yml"):
            continue
        if "acceptance-stage" not in fname and "prod-stage" not in fname:
            continue
        path = os.path.join(wf_dir, fname)
        try:
            with open(path, "r", encoding="utf-8") as f:
                content = f.read()
        except (OSError, UnicodeDecodeError):
            continue

        # Replace GITHUB_TOKEN with GHCR_TOKEN in promote-to-rc
        # and tag-docker-images env blocks. These are the steps that push
        # docker tags to cross-repo GHCR packages.
        #
        # In the acceptance stage, the pattern is:
        #   - name: Promote to RC
        #     ...
        #     env:
        #       GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        #
        # In the prod stage, there are two such steps:
        #   - Tag Docker Images for QA (qa-stage)
        #   - Tag Docker Images for Production (prod-stage)
        #
        # We need a targeted replacement: only replace GITHUB_TOKEN in the env
        # block of these specific steps. Since the workflow YAML has a
        # consistent pattern, we replace the specific line.
        new_content = content.replace(
            "GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}",
            "GITHUB_TOKEN: ${{ secrets.GHCR_TOKEN }}",
        )
        if new_content != content:
            with open(path, "w", encoding="utf-8", newline="\n") as f:
                f.write(new_content)


def fixup_multirepo_docker_compose(repo_dir: str, repo_name: str,
                                    frontend_repo: str, backend_repo: str,
                                    backend_lang: str) -> None:
    """Replace system repo name with component repo names in docker-compose files.

    Must run AFTER replace_repo_references, because at template-apply time
    the docker-compose still has 'optivem/starter' which gets replaced to
    'owner/repo' by replace_repo_references. This function then fixes
    'owner/repo' -> 'owner/repo-frontend' / 'owner/repo-backend'.
    """
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        for fname in filenames:
            if "docker-compose" not in fname or not fname.endswith(".yml"):
                continue
            path = os.path.join(dirpath, fname)
            replace_in_file(
                path,
                f"{repo_name}/multitier-frontend-react",
                f"{frontend_repo}/multitier-frontend-react",
            )
            replace_in_file(
                path,
                f"{repo_name}/multitier-backend-{backend_lang}",
                f"{backend_repo}/multitier-backend-{backend_lang}",
            )


def fixup_commit_stage_for_standalone(repo_dir: str, component: str, lang: str) -> None:
    """Adapt a commit stage workflow for a standalone component repo.

    When a component lives in its own repo (not under system/multitier/),
    we need to:
    - Remove path filters (the whole repo IS the component)
    - Update working-directory references
    - Update VERSION file path references
    - Update SonarCloud projectBaseDir
    """
    wf_dir = os.path.join(repo_dir, ".github", "workflows")
    if not os.path.isdir(wf_dir):
        return

    for fname in os.listdir(wf_dir):
        if not fname.endswith(".yml") or "commit-stage" not in fname:
            continue
        path = os.path.join(wf_dir, fname)

        # component is like "backend-java" or "frontend-react"
        old_path_prefix = f"system/multitier/{component}"

        try:
            with open(path, "r", encoding="utf-8") as f:
                lines = f.readlines()
        except (OSError, UnicodeDecodeError):
            continue

        new_lines = []
        skip_next_path_line = False
        i = 0
        while i < len(lines):
            line = lines[i]

            # Remove path filter blocks:
            #     paths:
            #         - 'system/multitier/backend-java/**'
            #         - '.github/workflows/...'
            if line.strip() == "paths:":
                # Skip paths: and all subsequent indented path lines
                i += 1
                while i < len(lines) and lines[i].strip().startswith("- '"):
                    i += 1
                continue

            # Replace working-directory references
            if f"working-directory: {old_path_prefix}" in line:
                line = line.replace(f"working-directory: {old_path_prefix}", "working-directory: .")
            # Replace VERSION file references
            if f"file: {old_path_prefix}/VERSION" in line:
                line = line.replace(f"file: {old_path_prefix}/VERSION", "file: VERSION")
            # Replace SonarCloud projectBaseDir
            if f"projectBaseDir: {old_path_prefix}" in line:
                line = line.replace(f"projectBaseDir: {old_path_prefix}", "projectBaseDir: .")

            new_lines.append(line)
            i += 1

        with open(path, "w", encoding="utf-8", newline="\n") as f:
            f.writelines(new_lines)
