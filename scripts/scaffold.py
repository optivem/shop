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

  Test mode (creates temp repo, verifies, optionally cleans up):
    python scaffold.py --owner acme --system-name "Page Turner" --repo page-turner \
        --arch monolith --lang java --test --cleanup

  Dry run (print actions without executing):
    python scaffold.py --owner acme --system-name "Page Turner" --repo page-turner \
        --arch monolith --lang java --dry-run

Environment variables (secrets — never on command line):
  DOCKERHUB_USERNAME  Docker Hub username
  DOCKERHUB_TOKEN     Docker Hub access token
  SONAR_TOKEN         SonarCloud token

Options:
  --owner          GitHub username or org (required)
  --system-name    System name, e.g. "Page Turner" (required)
  --repo           Repository name, e.g. "page-turner" (required)
  --arch           Architecture: monolith or multitier (required)
  --lang           System language: java, dotnet, typescript (monolith)
  --test-lang      Test language (defaults to --lang or --backend-lang)
  --backend-lang   Backend language (multitier)
  --frontend-lang  Frontend language (multitier, currently only: react)
  --random-suffix  Append 4-char hex suffix to repo name
  --dry-run        Print actions without executing
  --test           Test mode with optional cleanup
  --cleanup        Auto-cleanup in test mode (no prompt)
  --no-cleanup     Keep repo in test mode (no prompt)
  --workdir DIR    Working directory for cloning (default: temp dir)
  --help           Show this help
"""

import argparse
import json
import os
import re
import secrets
import shutil
import subprocess
import sys
import tempfile
import time
import urllib.request
import urllib.error

# ─── Colors ─────────────────────────────────────────────────────────────────

if sys.platform == "win32":
    os.system("")  # enable ANSI on Windows

CYAN = "\033[0;36m"
GREEN = "\033[0;32m"
YELLOW = "\033[0;33m"
RED = "\033[0;31m"
NC = "\033[0m"


def _print_safe(msg, file=None):
    """Print with fallback for terminals that can't handle Unicode."""
    try:
        print(msg, file=file)
    except UnicodeEncodeError:
        print(msg.encode("ascii", errors="replace").decode(), file=file)


def log(msg):
    _print_safe(f"{CYAN}>{NC} {msg}")


def ok(msg):
    _print_safe(f"{GREEN}OK{NC} {msg}")


def warn(msg):
    _print_safe(f"{YELLOW}WARN{NC} {msg}")


def fail(msg):
    _print_safe(f"{RED}FAIL{NC} {msg}")


def fatal(msg):
    _print_safe(f"{RED}FATAL:{NC} {msg}", file=sys.stderr)
    sys.exit(1)


# ─── Helpers ────────────────────────────────────────────────────────────────


def to_pascal_case(s):
    """Convert hyphenated string to PascalCase: 'page-turner' -> 'PageTurner'"""
    return "".join(part.capitalize() for part in s.split("-"))


def to_java_lower(s):
    """Convert hyphenated string to Java lowercase: 'page-turner' -> 'pageturner'"""
    return s.replace("-", "").lower()


def run(cmd, dry_run=False, check=True, capture=False, cwd=None):
    """Run a shell command. In dry-run mode, just print it."""
    if dry_run:
        log(f"[DRY RUN] {cmd}")
        return subprocess.CompletedProcess(cmd, 0, stdout="", stderr="")
    result = subprocess.run(
        cmd,
        shell=True,
        check=False,
        capture_output=capture,
        text=True,
        cwd=cwd,
    )
    if check and result.returncode != 0:
        stderr = result.stderr if capture else ""
        fatal(f"Command failed (exit {result.returncode}): {cmd}\n{stderr}")
    return result


def gh_api(method, endpoint, data=None, token=None):
    """Call the GitHub API using urllib (no external deps)."""
    url = f"https://api.github.com{endpoint}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, method=method)
    req.add_header("Accept", "application/vnd.github+json")
    if token:
        req.add_header("Authorization", f"token {token}")
    if body:
        req.add_header("Content-Type", "application/json")
    try:
        with urllib.request.urlopen(req) as resp:
            if resp.status == 204:
                return {}
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body_text = e.read().decode() if e.fp else ""
        return {"error": True, "status": e.code, "message": body_text}


def sonar_api(method, endpoint, data=None, token=None):
    """Call the SonarCloud API."""
    url = f"https://sonarcloud.io/api{endpoint}"
    if method == "POST" and data:
        body = "&".join(f"{k}={v}" for k, v in data.items()).encode()
    else:
        body = None
    req = urllib.request.Request(url, data=body, method=method)
    if token:
        import base64
        creds = base64.b64encode(f"{token}:".encode()).decode()
        req.add_header("Authorization", f"Basic {creds}")
    if body:
        req.add_header("Content-Type", "application/x-www-form-urlencoded")
    try:
        with urllib.request.urlopen(req) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body_text = e.read().decode() if e.fp else ""
        return {"error": True, "status": e.code, "message": body_text}


def replace_in_file(filepath, old, new):
    """Replace all occurrences of old with new in a file."""
    try:
        with open(filepath, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
    except (OSError, UnicodeDecodeError):
        return False
    if old not in content:
        return False
    content = content.replace(old, new)
    with open(filepath, "w", encoding="utf-8", newline="\n") as f:
        f.write(content)
    return True


def replace_in_tree(root, old, new, extensions=None):
    """Replace in all text files under root, optionally filtered by extension."""
    count = 0
    for dirpath, _dirnames, filenames in os.walk(root):
        if ".git" in dirpath.split(os.sep):
            continue
        for fname in filenames:
            if extensions and not any(fname.endswith(ext) for ext in extensions):
                continue
            filepath = os.path.join(dirpath, fname)
            if replace_in_file(filepath, old, new):
                count += 1
    return count


def rename_java_dirs(root, old_parts, new_parts):
    """Rename Java package directories: com/optivem/starter -> com/owner/repo.
    old_parts and new_parts are lists like ['com','optivem','starter'] and ['com','acme','pageturner'].
    """
    old_path = os.path.join(*old_parts)
    new_path = os.path.join(*new_parts)
    for dirpath, dirnames, filenames in os.walk(root):
        if old_path in dirpath:
            new_dirpath = dirpath.replace(old_path, new_path)
            os.makedirs(os.path.dirname(new_dirpath), exist_ok=True)
            if os.path.exists(dirpath) and not os.path.exists(new_dirpath):
                shutil.move(dirpath, new_dirpath)
            break  # restart walk since tree changed
    # Clean up empty old directories
    for dirpath, dirnames, filenames in os.walk(root, topdown=False):
        if old_parts[1] in dirpath.split(os.sep) and not filenames and not dirnames:
            try:
                os.rmdir(dirpath)
            except OSError:
                pass


def rename_dotnet_files(root, old_prefix, new_prefix):
    """Rename .NET files: Optivem.Starter.X.csproj -> NewNs.X.csproj etc."""
    for dirpath, _dirnames, filenames in os.walk(root):
        for fname in filenames:
            if old_prefix in fname:
                old_path = os.path.join(dirpath, fname)
                new_fname = fname.replace(old_prefix, new_prefix)
                new_path = os.path.join(dirpath, new_fname)
                os.rename(old_path, new_path)


# ─── Parse arguments ────────────────────────────────────────────────────────


def parse_args():
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
    parser.add_argument("--test", action="store_true", help="Test mode with optional cleanup")
    parser.add_argument("--cleanup", action="store_true", help="Auto-cleanup in test mode")
    parser.add_argument("--no-cleanup", action="store_true", help="Keep repo in test mode")
    parser.add_argument("--workdir", help="Working directory for cloning (default: temp dir)")
    return parser.parse_args()


# ─── Validation ─────────────────────────────────────────────────────────────


def validate(args):
    """Validate inputs and resolve defaults. Returns a config dict."""
    # Architecture-specific validation
    if args.arch == "monolith":
        if not args.lang:
            fatal("--lang is required for monolith architecture")
        lang = args.lang
        backend_lang = None
        frontend_lang = None
        test_lang = args.test_lang or lang
    elif args.arch == "multitier":
        if not args.backend_lang:
            fatal("--backend-lang is required for multitier architecture")
        if not args.frontend_lang:
            fatal("--frontend-lang is required for multitier architecture")
        lang = None
        backend_lang = args.backend_lang
        frontend_lang = args.frontend_lang
        test_lang = args.test_lang or backend_lang

    # Random suffix
    repo = args.repo
    if args.random_suffix:
        repo = f"{repo}-{secrets.token_hex(2)}"

    # Environment variables
    dockerhub_username = os.environ.get("DOCKERHUB_USERNAME", "")
    dockerhub_token = os.environ.get("DOCKERHUB_TOKEN", "")
    sonar_token = os.environ.get("SONAR_TOKEN", "")

    if not args.dry_run:
        if not dockerhub_username:
            fatal("DOCKERHUB_USERNAME environment variable is required")
        if not dockerhub_token:
            fatal("DOCKERHUB_TOKEN environment variable is required")
        if not sonar_token:
            fatal("SONAR_TOKEN environment variable is required")

    # Resolve starter path from script location
    script_dir = os.path.dirname(os.path.abspath(__file__))
    starter_path = os.path.dirname(script_dir)
    if not os.path.isfile(os.path.join(starter_path, "VERSION")):
        fatal(f"Cannot find VERSION file in {starter_path} — script must be inside starter/scripts/")

    # Workdir
    workdir = args.workdir or tempfile.mkdtemp(prefix="scaffold-")

    # Check gh CLI
    result = subprocess.run("gh auth status", shell=True, capture_output=True, text=True)
    if result.returncode != 0 and not args.dry_run:
        fatal("gh CLI is not authenticated. Run 'gh auth login' first.")

    # Namespace derivation
    owner = args.owner
    owner_pascal = to_pascal_case(owner) if "-" in owner else owner.capitalize()
    owner_lower = owner.lower()
    repo_pascal = to_pascal_case(repo)
    repo_nohyphens = to_java_lower(repo)
    full_repo = f"{owner}/{repo}"

    return {
        "owner": owner,
        "repo": repo,
        "full_repo": full_repo,
        "system_name": args.system_name,
        "arch": args.arch,
        "lang": lang,
        "backend_lang": backend_lang,
        "frontend_lang": frontend_lang,
        "test_lang": test_lang,
        "dry_run": args.dry_run,
        "test_mode": args.test,
        "cleanup": "yes" if args.cleanup else ("no" if args.no_cleanup else "ask"),
        "workdir": workdir,
        "starter_path": starter_path,
        "dockerhub_username": dockerhub_username,
        "dockerhub_token": dockerhub_token,
        "sonar_token": sonar_token,
        # Derived namespaces
        "owner_pascal": owner_pascal,
        "owner_lower": owner_lower,
        "repo_pascal": repo_pascal,
        "repo_nohyphens": repo_nohyphens,
        "java_ns_old": "com.optivem.starter",
        "java_ns_new": f"com.{owner_lower}.{repo_nohyphens}",
        "dotnet_ns_old": "Optivem.Starter",
        "dotnet_ns_new": f"{owner_pascal}.{repo_pascal}",
        "ts_pkg_old": "@optivem/starter-system-test",
        "ts_pkg_new": f"@{owner_lower}/{repo}-system-test",
    }


# ─── Step 1: Create repository ──────────────────────────────────────────────


def create_repo(cfg):
    log(f"Step 1: Creating repository {cfg['full_repo']}...")

    if cfg["dry_run"]:
        log(f"[DRY RUN] gh repo create {cfg['full_repo']} --public --add-readme --license mit")
        return

    # Check if repo already exists
    result = run(
        f"gh repo view {cfg['full_repo']} --json name",
        check=False, capture=True, dry_run=False,
    )
    if result.returncode == 0:
        warn(f"Repository {cfg['full_repo']} already exists — skipping creation")
        return

    run(f"gh repo create {cfg['full_repo']} --public --add-readme --license mit")
    time.sleep(3)  # Wait for GitHub to initialize
    ok(f"Created repository: {cfg['full_repo']}")


# ─── Step 2: Setup environments ─────────────────────────────────────────────


def setup_environments(cfg):
    log(f"Step 2: Creating environments...")

    for env in ["acceptance", "qa", "production"]:
        run(
            f"gh api repos/{cfg['full_repo']}/environments/{env} -X PUT",
            dry_run=cfg["dry_run"],
        )
    ok("Created environments: acceptance, qa, production")


# ─── Step 3: Setup secrets and variables ─────────────────────────────────────


def setup_secrets_and_variables(cfg):
    log("Step 3: Setting secrets and variables...")

    repo = cfg["full_repo"]
    dry = cfg["dry_run"]

    # Secrets (values masked in dry-run output)
    if dry:
        log("[DRY RUN] gh secret set DOCKERHUB_TOKEN --body *** --repo " + repo)
        log("[DRY RUN] gh secret set SONAR_TOKEN --body *** --repo " + repo)
        log("[DRY RUN] gh variable set DOCKERHUB_USERNAME --body *** --repo " + repo)
    else:
        run(f"gh secret set DOCKERHUB_TOKEN --body \"{cfg['dockerhub_token']}\" --repo {repo}")
        run(f"gh secret set SONAR_TOKEN --body \"{cfg['sonar_token']}\" --repo {repo}")
        run(f"gh variable set DOCKERHUB_USERNAME --body \"{cfg['dockerhub_username']}\" --repo {repo}")
    run(f"gh variable set SYSTEM_URL --body \"http://localhost:8080\" --repo {repo}", dry_run=dry)

    # Per-environment variables
    for env in ["acceptance", "qa", "production"]:
        run(f"gh variable set SYSTEM_URL --body \"http://localhost:8080\" --env {env} --repo {repo}", dry_run=dry)

    ok("Set secrets and variables")


# ─── Step 4: Clone and apply template ────────────────────────────────────────


def clone_and_apply_template(cfg):
    log("Step 4: Cloning repo and applying template files...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would clone repo and copy template files")
        return

    repo_dir = os.path.join(cfg["workdir"], "repo")
    starter = cfg["starter_path"]

    # Clone the target repo
    run(f"gh repo clone {cfg['full_repo']} \"{repo_dir}\"")
    ok(f"Cloned {cfg['full_repo']}")

    # Create directory structure
    os.makedirs(os.path.join(repo_dir, ".github", "workflows"), exist_ok=True)

    if cfg["arch"] == "monolith":
        _apply_monolith_template(cfg, repo_dir, starter)
    elif cfg["arch"] == "multitier":
        _apply_multitier_template(cfg, repo_dir, starter)

    cfg["repo_dir"] = repo_dir
    ok("Applied template files")


def _apply_monolith_template(cfg, repo_dir, starter):
    lang = cfg["lang"]
    test_lang = cfg["test_lang"]
    wf_src = os.path.join(starter, ".github", "workflows")
    wf_dst = os.path.join(repo_dir, ".github", "workflows")

    # Workflows
    workflows = [
        f"monolith-{lang}-commit-stage.yml",
        f"monolith-{test_lang}-acceptance-stage.yml",
        f"monolith-{test_lang}-qa-stage.yml",
        f"monolith-{test_lang}-qa-signoff.yml",
        f"monolith-{test_lang}-prod-stage.yml",
    ]
    if lang == test_lang:
        workflows.append(f"monolith-{lang}-verify.yml")

    for wf in workflows:
        src = os.path.join(wf_src, wf)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(wf_dst, wf))
        else:
            warn(f"Workflow not found: {wf}")

    # System code
    sys_src = os.path.join(starter, "system", "monolith", lang)
    sys_dst = os.path.join(repo_dir, "system", "monolith", lang)
    shutil.copytree(sys_src, sys_dst)

    # System tests
    test_src = os.path.join(starter, "system-test", test_lang)
    test_dst = os.path.join(repo_dir, "system-test", test_lang)
    shutil.copytree(test_src, test_dst)

    # Rename docker-compose: single for monolith
    compose_single = os.path.join(test_dst, "docker-compose.single.yml")
    compose_multi = os.path.join(test_dst, "docker-compose.multi.yml")
    compose_target = os.path.join(test_dst, "docker-compose.yml")
    if os.path.exists(compose_single):
        shutil.copy2(compose_single, compose_target)
        os.remove(compose_single)
    if os.path.exists(compose_multi):
        os.remove(compose_multi)

    # VERSION file
    version_src = os.path.join(starter, "VERSION")
    if os.path.exists(version_src):
        shutil.copy2(version_src, os.path.join(repo_dir, "VERSION"))


def _apply_multitier_template(cfg, repo_dir, starter):
    backend_lang = cfg["backend_lang"]
    frontend_lang = cfg["frontend_lang"]
    test_lang = cfg["test_lang"]
    wf_src = os.path.join(starter, ".github", "workflows")
    wf_dst = os.path.join(repo_dir, ".github", "workflows")

    # Workflows
    workflows = [
        f"multitier-backend-{backend_lang}-commit-stage.yml",
        f"multitier-frontend-{frontend_lang}-commit-stage.yml",
        f"multitier-system-{test_lang}-acceptance-stage.yml",
        f"multitier-system-{test_lang}-qa-stage.yml",
        f"multitier-system-{test_lang}-qa-signoff.yml",
        f"multitier-system-{test_lang}-prod-stage.yml",
    ]
    if backend_lang == test_lang:
        workflows.append(f"multitier-{backend_lang}-verify.yml")

    for wf in workflows:
        src = os.path.join(wf_src, wf)
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(wf_dst, wf))
        else:
            warn(f"Workflow not found: {wf}")

    # System code — backend + frontend
    be_src = os.path.join(starter, "system", "multitier", f"backend-{backend_lang}")
    be_dst = os.path.join(repo_dir, "system", "multitier", f"backend-{backend_lang}")
    shutil.copytree(be_src, be_dst)

    fe_src = os.path.join(starter, "system", "multitier", f"frontend-{frontend_lang}")
    fe_dst = os.path.join(repo_dir, "system", "multitier", f"frontend-{frontend_lang}")
    shutil.copytree(fe_src, fe_dst)

    # System tests
    test_src = os.path.join(starter, "system-test", test_lang)
    test_dst = os.path.join(repo_dir, "system-test", test_lang)
    shutil.copytree(test_src, test_dst)

    # Rename docker-compose: multi for multitier
    compose_single = os.path.join(test_dst, "docker-compose.single.yml")
    compose_multi = os.path.join(test_dst, "docker-compose.multi.yml")
    compose_target = os.path.join(test_dst, "docker-compose.yml")
    if os.path.exists(compose_multi):
        shutil.copy2(compose_multi, compose_target)
        os.remove(compose_multi)
    if os.path.exists(compose_single):
        os.remove(compose_single)

    # VERSION file
    version_src = os.path.join(starter, "VERSION")
    if os.path.exists(version_src):
        shutil.copy2(version_src, os.path.join(repo_dir, "VERSION"))


# ─── Step 5: Replace repository references ──────────────────────────────────


def replace_repo_references(cfg):
    log("Step 5: Replacing repository references...")

    if cfg["dry_run"]:
        log(f"[DRY RUN] Would replace optivem/starter → {cfg['full_repo']}")
        return

    repo_dir = cfg["repo_dir"]
    owner = cfg["owner"]
    repo = cfg["repo"]
    full_repo = cfg["full_repo"]
    owner_lower = cfg["owner_lower"]

    # All text file extensions to process
    text_exts = [
        ".yml", ".yaml", ".md", ".gradle", ".gradle.kts",
        ".csproj", ".sln", ".slnx", ".cshtml", ".json",
        ".cs", ".java", ".ts", ".tsx", ".js", ".jsx",
        ".xml", ".properties", ".cfg", ".txt",
    ]
    # Also match Dockerfile (no extension match needed, handle separately)

    # Pass 1: optivem/starter -> owner/repo
    n = replace_in_tree(repo_dir, "optivem/starter", full_repo, text_exts)
    # Also replace in Dockerfiles
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        for fname in filenames:
            if fname == "Dockerfile":
                replace_in_file(os.path.join(dirpath, fname), "optivem/starter", full_repo)
    ok(f"Pass 1: replaced optivem/starter → {full_repo} ({n}+ files)")

    # Pass 2: optivem_starter -> owner_repo (SonarCloud underscore variant)
    underscore_old = "optivem_starter"
    underscore_new = f"{owner}_{repo}"
    n = replace_in_tree(repo_dir, underscore_old, underscore_new, text_exts)
    ok(f"Pass 2: replaced {underscore_old} → {underscore_new} ({n} files)")

    # Pass 3: SonarCloud org replacement (scoped to avoid touching optivem/actions)
    # These are the exact patterns from the template files:
    sonar_replacements = [
        # Java build.gradle
        ("'sonar.organization', 'optivem'", f"'sonar.organization', '{owner_lower}'"),
        # .NET workflow yml
        ('/o:"optivem"', f'/o:"{owner_lower}"'),
        # TypeScript workflow yml
        ("-Dsonar.organization=optivem", f"-Dsonar.organization={owner_lower}"),
    ]
    for old, new in sonar_replacements:
        n = replace_in_tree(repo_dir, old, new)
        if n > 0:
            ok(f"Pass 3: replaced sonar org pattern ({n} files)")

    # Safety check: optivem/actions must still be intact
    actions_found = False
    wf_dir = os.path.join(repo_dir, ".github", "workflows")
    if os.path.isdir(wf_dir):
        for fname in os.listdir(wf_dir):
            filepath = os.path.join(wf_dir, fname)
            try:
                with open(filepath, "r", encoding="utf-8") as f:
                    if "optivem/actions" in f.read():
                        actions_found = True
                        break
            except (OSError, UnicodeDecodeError):
                pass

    if not actions_found:
        fatal("Safety check failed: optivem/actions references were corrupted during replacement!")

    ok("Safety check passed: optivem/actions references intact")

    # Lowercase docker-compose image URLs
    _lowercase_docker_compose_images(repo_dir)


def _lowercase_docker_compose_images(repo_dir):
    """Ensure ghcr.io image URLs in docker-compose files are lowercase."""
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        for fname in filenames:
            if "docker-compose" in fname and fname.endswith(".yml"):
                filepath = os.path.join(dirpath, fname)
                try:
                    with open(filepath, "r", encoding="utf-8") as f:
                        lines = f.readlines()
                    changed = False
                    for i, line in enumerate(lines):
                        if "image:" in line and "ghcr.io" in line:
                            # Lowercase everything after "image: "
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


# ─── Step 6: Replace namespaces ──────────────────────────────────────────────


def replace_namespaces(cfg):
    log("Step 6: Replacing namespaces...")

    if cfg["dry_run"]:
        log(f"[DRY RUN] Would replace language-specific namespaces")
        return

    repo_dir = cfg["repo_dir"]

    if cfg["arch"] == "monolith":
        _replace_namespaces_for_lang(cfg, repo_dir, cfg["lang"], "monolith")
        _replace_namespaces_for_lang(cfg, repo_dir, cfg["test_lang"], "systemtest")
    elif cfg["arch"] == "multitier":
        _replace_namespaces_for_lang(cfg, repo_dir, cfg["backend_lang"], "backend")
        _replace_namespaces_for_lang(cfg, repo_dir, cfg["test_lang"], "systemtest")
        # Frontend (react) has no namespace replacement needed

    ok("Namespace replacement complete")


def _replace_namespaces_for_lang(cfg, repo_dir, lang, component):
    """Replace namespaces for a specific language and component."""
    if lang == "java":
        _replace_java_namespaces(cfg, repo_dir, component)
    elif lang == "dotnet":
        _replace_dotnet_namespaces(cfg, repo_dir, component)
    elif lang == "typescript":
        _replace_typescript_namespaces(cfg, repo_dir, component)


def _replace_java_namespaces(cfg, repo_dir, component):
    """Replace Java package namespaces and rename directories."""
    old_ns = cfg["java_ns_old"]
    new_ns = cfg["java_ns_new"]

    # Map component to the template's component name
    # Template uses: com.optivem.starter.monolith, com.optivem.starter.backend, com.optivem.starter.systemtest
    old_full = f"{old_ns}.{component}"
    new_full = f"{new_ns}.{component}"

    # Replace in all Java-related files
    java_exts = [".java", ".gradle", ".gradle.kts", ".xml", ".properties"]
    n = replace_in_tree(repo_dir, old_full, new_full, java_exts)

    # Also replace in workflow yml files (test filter patterns)
    n += replace_in_tree(repo_dir, old_full, new_full, [".yml"])

    ok(f"Java: replaced {old_full} → {new_full} ({n} files)")

    # Rename directories: com/optivem/starter -> com/owner/repo
    old_dir_parts = ["com", "optivem", "starter"]
    new_dir_parts = ["com", cfg["owner_lower"], cfg["repo_nohyphens"]]

    # Find all src dirs that contain the old path
    for dirpath, dirnames, _filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        src_path = os.path.join(dirpath, *old_dir_parts)
        if os.path.isdir(src_path):
            rename_java_dirs(dirpath, old_dir_parts, new_dir_parts)

    ok(f"Java: renamed directories com/optivem/starter → com/{cfg['owner_lower']}/{cfg['repo_nohyphens']}")


def _replace_dotnet_namespaces(cfg, repo_dir, component):
    """Replace .NET namespaces and rename files."""
    old_ns = cfg["dotnet_ns_old"]
    new_ns = cfg["dotnet_ns_new"]

    # Map component to the template's component name
    component_map = {
        "monolith": "Monolith",
        "backend": "Backend",
        "systemtest": "SystemTest",
    }
    dotnet_component = component_map[component]

    old_full = f"{old_ns}.{dotnet_component}"
    new_full = f"{new_ns}.{dotnet_component}"

    # Replace in all .NET-related files
    dotnet_exts = [".cs", ".cshtml", ".csproj", ".sln", ".slnx", ".json", ".yml"]
    n = replace_in_tree(repo_dir, old_full, new_full, dotnet_exts)

    # Also replace in Dockerfiles
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        for fname in filenames:
            if fname == "Dockerfile":
                if replace_in_file(os.path.join(dirpath, fname), old_full, new_full):
                    n += 1

    ok(f".NET: replaced {old_full} → {new_full} ({n} files)")

    # Rename files
    rename_dotnet_files(repo_dir, old_full, new_full)
    ok(f".NET: renamed files {old_full}.* → {new_full}.*")


def _replace_typescript_namespaces(cfg, repo_dir, component):
    """Replace TypeScript namespaces in package.json."""
    if component != "systemtest":
        return  # Only system-test has a scoped package name

    old_pkg = cfg["ts_pkg_old"]
    new_pkg = cfg["ts_pkg_new"]

    n = replace_in_tree(repo_dir, old_pkg, new_pkg, [".json"])
    ok(f"TypeScript: replaced {old_pkg} → {new_pkg} ({n} files)")

    # Update author and description in system-test package.json
    # Find the system-test package.json
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if "system-test" in dirpath and "package.json" in filenames:
            pkg_path = os.path.join(dirpath, "package.json")
            replace_in_file(pkg_path, '"author": "Optivem"', f'"author": "{cfg["owner"]}"')
            replace_in_file(pkg_path, '"Starter - System Tests"', f'"{cfg["system_name"]} - System Tests"')
            # Replace optivem keyword
            replace_in_file(pkg_path, '"optivem"', f'"{cfg["owner_lower"]}"')
            ok(f"TypeScript: updated package.json metadata")
            break

    # Also update monolith/typescript or backend-typescript package.json name if present
    for dirpath, _dirnames, filenames in os.walk(repo_dir):
        if ".git" in dirpath.split(os.sep):
            continue
        if "package.json" in filenames and "system-test" not in dirpath and "node_modules" not in dirpath:
            pkg_path = os.path.join(dirpath, "package.json")
            # Update the name field for system components
            if "monolith" in dirpath:
                replace_in_file(pkg_path, '"name": "starter-monolith"', f'"name": "{cfg["repo"]}-monolith"')
            elif "backend" in dirpath:
                replace_in_file(pkg_path, '"name": "starter-backend"', f'"name": "{cfg["repo"]}-backend"')


# ─── Step 7: Update README ──────────────────────────────────────────────────


def update_readme(cfg):
    log("Step 7: Generating README...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would generate README.md")
        return

    repo_dir = cfg["repo_dir"]
    full_repo = cfg["full_repo"]
    system_name = cfg["system_name"]

    badges = _generate_badges(cfg)

    readme = f"# {system_name}\n\n"
    readme += badges + "\n"
    readme += "## License\n\n"
    readme += "MIT License\n\n"
    readme += "## Contributors\n\n"
    readme += f"- [{cfg['owner']}](https://github.com/{cfg['owner']})\n"

    readme_path = os.path.join(repo_dir, "README.md")
    with open(readme_path, "w", encoding="utf-8", newline="\n") as f:
        f.write(readme)

    ok("Generated README.md")


def _generate_badges(cfg):
    full_repo = cfg["full_repo"]
    base = f"https://github.com/{full_repo}/actions/workflows"
    badges = []

    if cfg["arch"] == "monolith":
        lang = cfg["lang"]
        test_lang = cfg["test_lang"]
        badge_workflows = [
            (f"monolith-{lang}-commit-stage.yml", "commit-stage"),
            (f"monolith-{test_lang}-acceptance-stage.yml", "acceptance-stage"),
            (f"monolith-{test_lang}-qa-stage.yml", "qa-stage"),
            (f"monolith-{test_lang}-qa-signoff.yml", "qa-signoff"),
            (f"monolith-{test_lang}-prod-stage.yml", "prod-stage"),
        ]
    elif cfg["arch"] == "multitier":
        backend_lang = cfg["backend_lang"]
        frontend_lang = cfg["frontend_lang"]
        test_lang = cfg["test_lang"]
        badge_workflows = [
            (f"multitier-backend-{backend_lang}-commit-stage.yml", "backend-commit-stage"),
            (f"multitier-frontend-{frontend_lang}-commit-stage.yml", "frontend-commit-stage"),
            (f"multitier-system-{test_lang}-acceptance-stage.yml", "acceptance-stage"),
            (f"multitier-system-{test_lang}-qa-stage.yml", "qa-stage"),
            (f"multitier-system-{test_lang}-qa-signoff.yml", "qa-signoff"),
            (f"multitier-system-{test_lang}-prod-stage.yml", "prod-stage"),
        ]

    for wf_file, label in badge_workflows:
        badges.append(f"[![{label}]({base}/{wf_file}/badge.svg)]({base}/{wf_file})")

    return "\n".join(badges) + "\n"


# ─── Step 8: Create SonarCloud projects ──────────────────────────────────────


def create_sonarcloud_projects(cfg):
    log("Step 8: Creating SonarCloud projects...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would create SonarCloud org and project(s)")
        return

    token = cfg["sonar_token"]
    owner = cfg["owner_lower"]
    repo = cfg["repo"]

    # Create organization
    result = sonar_api("POST", "/organizations/create", {"key": owner, "name": owner}, token)
    if result.get("error") and "already exist" not in result.get("message", "").lower():
        warn(f"SonarCloud org creation: {result.get('message', 'unknown error')}")
    else:
        ok(f"SonarCloud org: {owner}")

    # Determine project keys based on architecture
    project_keys = _get_sonar_project_keys(cfg)

    for key in project_keys:
        # Create project
        result = sonar_api("POST", "/projects/create", {
            "organization": owner,
            "project": key,
            "name": key,
        }, token)
        if result.get("error") and "already exist" not in result.get("message", "").lower():
            warn(f"SonarCloud project {key}: {result.get('message', 'unknown error')}")
        else:
            ok(f"SonarCloud project: {key}")

        # Rename default branch master -> main
        result = sonar_api("POST", "/project_branches/rename", {
            "project": key,
            "name": "main",
        }, token)
        if result.get("error") and "already exists" not in result.get("message", "").lower():
            warn(f"SonarCloud branch rename for {key}: {result.get('message', 'unknown error')}")


def _get_sonar_project_keys(cfg):
    """Deterministically compute SonarCloud project keys."""
    owner = cfg["owner"]
    repo = cfg["repo"]
    prefix = f"{owner}_{repo}"

    if cfg["arch"] == "monolith":
        lang = cfg["lang"]
        return [f"{prefix}-monolith-{lang}"]
    elif cfg["arch"] == "multitier":
        backend_lang = cfg["backend_lang"]
        frontend_lang = cfg["frontend_lang"]
        return [
            f"{prefix}-multitier-backend-{backend_lang}",
            f"{prefix}-multitier-frontend-{frontend_lang}",
        ]


# ─── Step 9: Commit and push ────────────────────────────────────────────────


def commit_and_push(cfg):
    log("Step 9: Committing and pushing...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would git add, commit, push")
        return

    repo_dir = cfg["repo_dir"]
    run("git add -A", cwd=repo_dir)
    run('git commit -m "Apply pipeline template"', cwd=repo_dir)
    run("git push", cwd=repo_dir)
    ok(f"Pushed template to {cfg['full_repo']}")


# ─── Step 10: Verify commit stage ───────────────────────────────────────────


def verify_commit_stage(cfg):
    log("Step 10: Verifying commit stage workflow...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would wait for commit stage workflow")
        return

    repo = cfg["full_repo"]
    time.sleep(5)  # Wait for GitHub to register the push

    result = run(
        f"gh run watch --repo {repo} --exit-status",
        check=False, capture=True,
    )
    if result.returncode != 0:
        fail("Commit stage failed!")
        # Show failed logs
        run(f"gh run list --repo {repo} --limit 1 --json databaseId --jq '.[0].databaseId'",
            capture=True, check=False)
        fatal(f"Commit stage workflow failed. Check: https://github.com/{repo}/actions")

    ok("Commit stage passed!")


# ─── Step 11: Verify acceptance stage ────────────────────────────────────────


def verify_acceptance_stage(cfg):
    log("Step 11: Triggering and verifying acceptance stage...")

    if cfg["dry_run"]:
        log("[DRY RUN] Would trigger and wait for acceptance stage workflow")
        return

    repo = cfg["full_repo"]
    test_lang = cfg["test_lang"]

    if cfg["arch"] == "monolith":
        wf = f"monolith-{test_lang}-acceptance-stage.yml"
    elif cfg["arch"] == "multitier":
        wf = f"multitier-system-{test_lang}-acceptance-stage.yml"

    run(f"gh workflow run {wf} --repo {repo}")
    time.sleep(5)  # Wait for workflow to register

    result = run(
        f"gh run watch --repo {repo} --exit-status",
        check=False, capture=True,
    )
    if result.returncode != 0:
        fail("Acceptance stage failed!")
        fatal(f"Acceptance stage workflow failed. Check: https://github.com/{repo}/actions")

    ok("Acceptance stage passed!")


# ─── Step 12: Cleanup (test mode only) ──────────────────────────────────────


def cleanup(cfg):
    if not cfg["test_mode"]:
        return

    should_cleanup = cfg["cleanup"]
    if should_cleanup == "ask":
        answer = input(f"\nDelete test repository {cfg['full_repo']}? [y/N] ").strip().lower()
        should_cleanup = "yes" if answer in ("y", "yes") else "no"

    if should_cleanup == "yes":
        log(f"Cleaning up: deleting {cfg['full_repo']}...")
        run(f"gh repo delete {cfg['full_repo']} --yes", check=False)
        ok(f"Deleted repository {cfg['full_repo']}")

        # Delete SonarCloud projects
        token = cfg["sonar_token"]
        for key in _get_sonar_project_keys(cfg):
            sonar_api("POST", "/projects/delete", {"project": key}, token)
            ok(f"Deleted SonarCloud project: {key}")

        # Clean up workdir
        repo_dir = cfg.get("repo_dir")
        if repo_dir and os.path.exists(repo_dir):
            shutil.rmtree(repo_dir, ignore_errors=True)
        ok("Cleanup complete")
    else:
        log(f"Keeping repository: https://github.com/{cfg['full_repo']}")


# ─── Main ───────────────────────────────────────────────────────────────────


def main():
    args = parse_args()
    cfg = validate(args)

    print()
    print("=" * 42)
    print("  Pipeline Project Setup")
    print("=" * 42)
    print()
    log(f"Owner:       {cfg['owner']}")
    log(f"Repo:        {cfg['repo']}")
    log(f"System:      {cfg['system_name']}")
    log(f"Arch:        {cfg['arch']}")
    if cfg["arch"] == "monolith":
        log(f"Language:    {cfg['lang']}")
    else:
        log(f"Backend:     {cfg['backend_lang']}")
        log(f"Frontend:    {cfg['frontend_lang']}")
    log(f"Test lang:   {cfg['test_lang']}")
    log(f"Dry run:     {cfg['dry_run']}")
    log(f"Test mode:   {cfg['test_mode']}")
    log(f"Workdir:     {cfg['workdir']}")
    print()

    errors = 0
    steps = [
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

    for step_name, step_func in steps:
        try:
            step_func(cfg)
        except SystemExit:
            errors += 1
            break
        except Exception as e:
            fail(f"Step failed: {step_name} — {e}")
            errors += 1
            break

    print()
    print("=" * 42)

    if errors > 0:
        fail(f"Setup completed with {errors} error(s)")
    else:
        ok("All steps passed!")

    print()
    print(f"  Repository: https://github.com/{cfg['full_repo']}")
    print(f"  Actions:    https://github.com/{cfg['full_repo']}/actions")
    print()

    cleanup(cfg)

    sys.exit(errors)


if __name__ == "__main__":
    main()
