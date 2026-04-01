// Package steps implements the scaffold pipeline steps.
package steps

import (
	"os"
	"path/filepath"
	"time"

	"github.com/optivem/gh-scaffold/internal/config"
	"github.com/optivem/gh-scaffold/internal/log"
	"github.com/optivem/gh-scaffold/internal/shell"
)

// CreateRepos creates the GitHub repository (and component repos for multitier).
func CreateRepos(cfg *config.Config, gh *shell.GitHub) {
	log.Logf("Step 1: Creating repository %s...", cfg.FullRepo)

	if cfg.DryRun {
		log.Logf("[DRY RUN] gh repo create %s --public --add-readme --license mit", cfg.FullRepo)
		if cfg.Arch == "multitier" {
			log.Logf("[DRY RUN] gh repo create %s --public --add-readme --license mit", cfg.FrontendFullRepo)
			log.Logf("[DRY RUN] gh repo create %s --public --add-readme --license mit", cfg.BackendFullRepo)
		}
		return
	}

	gh.CreateRepo()
	time.Sleep(3 * time.Second)
	log.OKf("Created repository: %s", cfg.FullRepo)

	if cfg.Arch == "multitier" {
		ghFrontend := gh.ForRepo(cfg.FrontendFullRepo)
		ghBackend := gh.ForRepo(cfg.BackendFullRepo)

		ghFrontend.CreateRepo()
		time.Sleep(3 * time.Second)
		log.OKf("Created repository: %s", cfg.FrontendFullRepo)

		ghBackend.CreateRepo()
		time.Sleep(3 * time.Second)
		log.OKf("Created repository: %s", cfg.BackendFullRepo)
	}
}

// SetupEnvironments creates GitHub environments on the main repo.
func SetupEnvironments(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 2: Creating environments...")

	lang := cfg.Lang
	if cfg.Arch == "multitier" {
		lang = cfg.BackendLang
	}
	prefix := cfg.Arch + "-" + lang

	for _, stage := range []string{"acceptance", "qa", "production"} {
		envName := prefix + "-" + stage
		gh.CreateEnvironment(envName)
	}
	log.OKf("Created environments: %s-acceptance, %s-qa, %s-production", prefix, prefix, prefix)
}

// SetupSecretsAndVariables sets GitHub Actions secrets and variables.
func SetupSecretsAndVariables(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 3: Setting secrets and variables...")

	gh.SecretSet("DOCKERHUB_TOKEN", cfg.DockerHubToken)
	gh.SecretSet("SONAR_TOKEN", cfg.SonarToken)
	gh.VariableSet("DOCKERHUB_USERNAME", cfg.DockerHubUsername)

	if cfg.Arch == "multitier" {
		gh.SecretSet("GHCR_TOKEN", cfg.GHCRToken)

		for _, fullRepo := range []string{cfg.FrontendFullRepo, cfg.BackendFullRepo} {
			ghComp := gh.ForRepo(fullRepo)
			ghComp.SecretSet("DOCKERHUB_TOKEN", cfg.DockerHubToken)
			ghComp.SecretSet("SONAR_TOKEN", cfg.SonarToken)
			ghComp.VariableSet("DOCKERHUB_USERNAME", cfg.DockerHubUsername)
		}
		log.OK("Set secrets and variables on component repositories")
	}

	log.OK("Set secrets and variables")
}

// CloneRepos clones the repository (and component repos for multitier).
func CloneRepos(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 4: Cloning repo(s)...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would clone repo(s)")
		return
	}

	repoDir := filepath.Join(cfg.WorkDir, "repo")
	gh.Clone(repoDir)
	cfg.RepoDir = repoDir
	log.OKf("Cloned %s", cfg.FullRepo)

	if cfg.Arch == "multitier" {
		frontendDir := filepath.Join(cfg.WorkDir, "repo-frontend")
		backendDir := filepath.Join(cfg.WorkDir, "repo-backend")

		gh.ForRepo(cfg.FrontendFullRepo).Clone(frontendDir)
		cfg.FrontendRepoDir = frontendDir
		log.OKf("Cloned %s", cfg.FrontendFullRepo)

		gh.ForRepo(cfg.BackendFullRepo).Clone(backendDir)
		cfg.BackendRepoDir = backendDir
		log.OKf("Cloned %s", cfg.BackendFullRepo)
	}
}

// EnsureWorkflowDir creates the .github/workflows directory in a repo.
func EnsureWorkflowDir(repoDir string) {
	os.MkdirAll(filepath.Join(repoDir, ".github", "workflows"), 0755)
}
