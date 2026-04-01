package steps

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/optivem/gh-scaffold/internal/config"
	"github.com/optivem/gh-scaffold/internal/log"
	"github.com/optivem/gh-scaffold/internal/shell"
)

// GetSonarProjectKeys returns the SonarCloud project keys for the given config.
func GetSonarProjectKeys(cfg *config.Config) []string {
	prefix := cfg.Owner + "_" + cfg.Repo
	if cfg.Arch == "monolith" {
		return []string{prefix + "-monolith-" + cfg.Lang}
	}
	return []string{
		cfg.Owner + "_" + cfg.BackendRepo + "-multitier-backend-" + cfg.BackendLang,
		cfg.Owner + "_" + cfg.FrontendRepo + "-multitier-frontend-" + cfg.FrontendLang,
	}
}

// UpdateReadme generates README.md for the repo(s).
func UpdateReadme(cfg *config.Config) {
	log.Log("Step 8: Generating README...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would generate README.md")
		return
	}

	if cfg.Arch == "monolith" {
		badges := generateBadges(cfg)
		writeReadme(cfg.RepoDir, cfg.SystemName, badges, cfg.Owner)
	} else {
		writeSystemReadme(cfg)
		writeComponentReadme(
			cfg.BackendRepoDir, cfg.SystemName, "Backend",
			cfg.BackendFullRepo, cfg.BackendLang, "backend", cfg.Owner,
		)
		writeComponentReadme(
			cfg.FrontendRepoDir, cfg.SystemName, "Frontend",
			cfg.FrontendFullRepo, cfg.FrontendLang, "frontend", cfg.Owner,
		)
	}

	log.OK("Generated README.md")
}

func writeReadme(repoDir, title, badges, owner string) {
	content := fmt.Sprintf("# %s\n\n%s\n## License\n\nMIT License\n\n## Contributors\n\n- [%s](https://github.com/%s)\n",
		title, badges, owner, owner)
	os.WriteFile(filepath.Join(repoDir, "README.md"), []byte(content), 0644)
}

func writeSystemReadme(cfg *config.Config) {
	bl, fl, tl := cfg.BackendLang, cfg.FrontendLang, cfg.TestLang
	base := "https://github.com/" + cfg.FullRepo + "/actions/workflows"
	backendBase := "https://github.com/" + cfg.BackendFullRepo + "/actions/workflows"
	frontendBase := "https://github.com/" + cfg.FrontendFullRepo + "/actions/workflows"

	badgeItems := [][2]string{
		{backendBase + "/multitier-backend-" + bl + "-commit-stage.yml", "backend-commit-stage"},
		{frontendBase + "/multitier-frontend-" + fl + "-commit-stage.yml", "frontend-commit-stage"},
		{base + "/multitier-system-" + tl + "-acceptance-stage.yml", "acceptance-stage"},
		{base + "/multitier-system-" + tl + "-qa-stage.yml", "qa-stage"},
		{base + "/multitier-system-" + tl + "-qa-signoff.yml", "qa-signoff"},
		{base + "/multitier-system-" + tl + "-prod-stage.yml", "prod-stage"},
	}

	var badges strings.Builder
	for _, item := range badgeItems {
		fmt.Fprintf(&badges, "[![%s](%s/badge.svg)](%s)\n", item[1], item[0], item[0])
	}

	reposSection := fmt.Sprintf("## Repositories\n\n- [%s](https://github.com/%s) — Backend (%s)\n- [%s](https://github.com/%s) — Frontend (%s)\n",
		cfg.BackendRepo, cfg.BackendFullRepo, bl,
		cfg.FrontendRepo, cfg.FrontendFullRepo, fl)

	content := fmt.Sprintf("# %s\n\n%s\n%s\n## License\n\nMIT License\n\n## Contributors\n\n- [%s](https://github.com/%s)\n",
		cfg.SystemName, badges.String(), reposSection, cfg.Owner, cfg.Owner)
	os.WriteFile(filepath.Join(cfg.RepoDir, "README.md"), []byte(content), 0644)
}

func writeComponentReadme(repoDir, systemName, componentLabel, fullRepo, lang, componentType, owner string) {
	wfName := "multitier-" + componentType + "-" + lang + "-commit-stage.yml"
	base := "https://github.com/" + fullRepo + "/actions/workflows"
	badges := fmt.Sprintf("[![commit-stage](%s/%s/badge.svg)](%s/%s)\n", base, wfName, base, wfName)

	content := fmt.Sprintf("# %s — %s\n\n%s\n## License\n\nMIT License\n\n## Contributors\n\n- [%s](https://github.com/%s)\n",
		systemName, componentLabel, badges, owner, owner)
	os.WriteFile(filepath.Join(repoDir, "README.md"), []byte(content), 0644)
}

func generateBadges(cfg *config.Config) string {
	base := "https://github.com/" + cfg.FullRepo + "/actions/workflows"

	var items [][2]string
	if cfg.Arch == "monolith" {
		items = [][2]string{
			{"monolith-" + cfg.Lang + "-commit-stage.yml", "commit-stage"},
			{"monolith-" + cfg.TestLang + "-acceptance-stage.yml", "acceptance-stage"},
			{"monolith-" + cfg.TestLang + "-qa-stage.yml", "qa-stage"},
			{"monolith-" + cfg.TestLang + "-qa-signoff.yml", "qa-signoff"},
			{"monolith-" + cfg.TestLang + "-prod-stage.yml", "prod-stage"},
		}
	} else {
		bl, fl, tl := cfg.BackendLang, cfg.FrontendLang, cfg.TestLang
		items = [][2]string{
			{"multitier-backend-" + bl + "-commit-stage.yml", "backend-commit-stage"},
			{"multitier-frontend-" + fl + "-commit-stage.yml", "frontend-commit-stage"},
			{"multitier-system-" + tl + "-acceptance-stage.yml", "acceptance-stage"},
			{"multitier-system-" + tl + "-qa-stage.yml", "qa-stage"},
			{"multitier-system-" + tl + "-qa-signoff.yml", "qa-signoff"},
			{"multitier-system-" + tl + "-prod-stage.yml", "prod-stage"},
		}
	}

	var b strings.Builder
	for _, item := range items {
		fmt.Fprintf(&b, "[![%s](%s/%s/badge.svg)](%s/%s)\n", item[1], base, item[0], base, item[0])
	}
	return b.String()
}

// CreateSonarCloudProjects creates SonarCloud org and projects.
func CreateSonarCloudProjects(cfg *config.Config, sc *shell.SonarCloud) {
	log.Log("Step 9: Creating SonarCloud projects...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would create SonarCloud org and project(s)")
		return
	}

	sc.CreateOrg()
	for _, key := range GetSonarProjectKeys(cfg) {
		sc.CreateProject(key)
	}
}

// CommitAndPush commits and pushes changes to GitHub.
func CommitAndPush(cfg *config.Config) {
	log.Log("Step 10: Committing and pushing...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would git add, commit, push")
		return
	}

	commitAndPushRepo(cfg.RepoDir, cfg.FullRepo)

	if cfg.Arch == "multitier" {
		commitAndPushRepo(cfg.BackendRepoDir, cfg.BackendFullRepo)
		commitAndPushRepo(cfg.FrontendRepoDir, cfg.FrontendFullRepo)
	}
}

func commitAndPushRepo(repoDir, fullRepo string) {
	shell.Run("git add -A", false, true, repoDir)
	shell.Run(`git commit -m "Apply pipeline template"`, false, true, repoDir)
	shell.Run("git push", false, true, repoDir)
	log.OKf("Pushed template to %s", fullRepo)
}

// VerifyCommitStage waits for commit stage workflow to pass.
func VerifyCommitStage(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 11: Verifying commit stage workflow...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would wait for commit stage workflow")
		return
	}

	time.Sleep(5 * time.Second)

	if cfg.Arch == "monolith" {
		verifyWorkflow(gh, "Commit stage", "", nil)
	} else {
		ghBackend := gh.ForRepo(cfg.BackendFullRepo)
		ghFrontend := gh.ForRepo(cfg.FrontendFullRepo)
		verifyWorkflow(ghBackend, "Backend commit stage", "", nil)
		verifyWorkflow(ghFrontend, "Frontend commit stage", "", nil)
	}
}

// VerifyAcceptanceStage triggers and verifies acceptance stage.
func VerifyAcceptanceStage(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 12: Triggering and verifying acceptance stage...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would trigger and wait for acceptance stage workflow")
		return
	}

	var wf string
	if cfg.Arch == "monolith" {
		wf = "monolith-" + cfg.TestLang + "-acceptance-stage.yml"
	} else {
		wf = "multitier-system-" + cfg.TestLang + "-acceptance-stage.yml"
	}

	verifyWorkflow(gh, "Acceptance stage", wf, nil)

	rcVersion := getRCVersion(gh)
	if rcVersion != "" {
		cfg.RCVersion = rcVersion
		log.OKf("RC version: %s", rcVersion)
	} else {
		log.Fatal("Could not determine RC version from acceptance stage run")
	}
}

// VerifyQAStage triggers and verifies QA stage.
func VerifyQAStage(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 13: Triggering and verifying QA stage...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would trigger and wait for QA stage workflow")
		return
	}

	if cfg.RCVersion == "" {
		log.Fatal("No RC version available — acceptance stage must run first")
	}

	var wf string
	if cfg.Arch == "monolith" {
		wf = "monolith-" + cfg.TestLang + "-qa-stage.yml"
	} else {
		wf = "multitier-system-" + cfg.TestLang + "-qa-stage.yml"
	}

	verifyWorkflow(gh, "QA stage", wf, map[string]string{"version": cfg.RCVersion})
}

// VerifyQASignoff triggers and verifies QA signoff.
func VerifyQASignoff(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 14: Triggering and verifying QA signoff...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would trigger and wait for QA signoff workflow")
		return
	}

	if cfg.RCVersion == "" {
		log.Fatal("No RC version available — acceptance stage must run first")
	}

	var wf string
	if cfg.Arch == "monolith" {
		wf = "monolith-" + cfg.TestLang + "-qa-signoff.yml"
	} else {
		wf = "multitier-system-" + cfg.TestLang + "-qa-signoff.yml"
	}

	verifyWorkflow(gh, "QA signoff", wf, map[string]string{"version": cfg.RCVersion, "result": "approved"})
}

// VerifyProdStage triggers and verifies production stage.
func VerifyProdStage(cfg *config.Config, gh *shell.GitHub) {
	log.Log("Step 15: Triggering and verifying production stage...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would trigger and wait for production stage workflow")
		return
	}

	if cfg.RCVersion == "" {
		log.Fatal("No RC version available — acceptance stage must run first")
	}

	var wf string
	if cfg.Arch == "monolith" {
		wf = "monolith-" + cfg.TestLang + "-prod-stage.yml"
	} else {
		wf = "multitier-system-" + cfg.TestLang + "-prod-stage.yml"
	}

	verifyWorkflow(gh, "Production stage", wf, map[string]string{"version": cfg.RCVersion})
}

func verifyWorkflow(gh *shell.GitHub, label, triggerWorkflow string, fields map[string]string) {
	shell.CheckRateLimit()
	if triggerWorkflow != "" {
		gh.WorkflowRun(triggerWorkflow, fields)
		time.Sleep(5 * time.Second)
	}

	shell.CheckRateLimit()
	err := gh.RunWatch()
	if err != nil {
		log.Failf("%s failed!", label)
		log.Fatalf("%s workflow failed. Check: https://github.com/%s/actions", label, gh.Repo)
	}
	log.OKf("%s passed!", label)
}

func getRCVersion(gh *shell.GitHub) string {
	shell.CheckRateLimit()

	out, err := shell.RunCapture(
		fmt.Sprintf("gh api repos/%s/releases --jq .[0].tag_name", gh.Repo), "")
	if err == nil && strings.Contains(out, "-rc.") {
		return out
	}

	// Fallback: parse JSON
	out, err = shell.RunCapture(
		fmt.Sprintf("gh api repos/%s/releases", gh.Repo), "")
	if err == nil {
		var releases []struct {
			TagName string `json:"tag_name"`
		}
		if json.Unmarshal([]byte(out), &releases) == nil && len(releases) > 0 {
			if strings.Contains(releases[0].TagName, "-rc.") {
				return releases[0].TagName
			}
		}
	}

	return ""
}
