"""Re-export all step functions for the main entry point."""

from .github_setup import create_repos, setup_environments, setup_secrets_and_variables
from .apply_template import clone_repos, apply_template
from .replacements import replace_repo_references, replace_namespaces
from .finalize import (
    update_readme,
    create_sonarcloud_projects,
    commit_and_push,
    verify_commit_stage,
    verify_acceptance_stage,
    verify_qa_stage,
    verify_qa_signoff,
    verify_prod_stage,
    get_sonar_project_keys,
)

__all__ = [
    "create_repos",
    "setup_environments",
    "setup_secrets_and_variables",
    "clone_repos",
    "apply_template",
    "replace_repo_references",
    "replace_namespaces",
    "update_readme",
    "create_sonarcloud_projects",
    "commit_and_push",
    "verify_commit_stage",
    "verify_acceptance_stage",
    "verify_qa_stage",
    "verify_qa_signoff",
    "verify_prod_stage",
    "get_sonar_project_keys",
]
