package steps

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/optivem/gh-scaffold/internal/config"
	"github.com/optivem/gh-scaffold/internal/files"
	"github.com/optivem/gh-scaffold/internal/log"
	"github.com/optivem/gh-scaffold/internal/templates"
)

// Internal port exposed by each language's Docker image.
var internalPorts = map[string]int{
	"java": 8080, "dotnet": 8080, "typescript": 3000,
}

// ApplyTemplate copies template files into the cloned repo(s).
func ApplyTemplate(cfg *config.Config) {
	log.Log("Step 5: Applying template files...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would apply template files")
		return
	}

	EnsureWorkflowDir(cfg.RepoDir)

	if cfg.Arch == "monolith" {
		applyMonolith(cfg)
	} else {
		applyMultitier(cfg)
	}

	log.OK("Applied template files")
}

func applyMonolith(cfg *config.Config) {
	lang := cfg.Lang
	testLang := cfg.TestLang
	starter := cfg.StarterPath
	repoDir := cfg.RepoDir

	workflows := []string{
		"monolith-" + lang + "-commit-stage.yml",
		"monolith-" + testLang + "-acceptance-stage.yml",
		"monolith-" + testLang + "-qa-stage.yml",
		"monolith-" + testLang + "-qa-signoff.yml",
		"monolith-" + testLang + "-prod-stage.yml",
	}
	if lang == testLang {
		workflows = append(workflows, "monolith-"+lang+"-verify.yml")
	}
	templates.CopyWorkflows(workflows, starter, repoDir)

	// Copy system code
	files.CopyDir(
		filepath.Join(starter, "system", "monolith", lang),
		filepath.Join(repoDir, "system", "monolith", lang),
	)

	// Copy system tests
	testDst := filepath.Join(repoDir, "system-test", testLang)
	files.CopyDir(filepath.Join(starter, "system-test", testLang), testDst)
	templates.SelectDockerCompose(testDst, "single")
	templates.CopyVersion(starter, repoDir)

	// Cross-language fixup
	if lang != testLang {
		fixupMonolithCrossLang(repoDir, lang, testLang)
	}
}

func applyMultitier(cfg *config.Config) {
	backendLang := cfg.BackendLang
	frontendLang := cfg.FrontendLang
	testLang := cfg.TestLang
	starter := cfg.StarterPath
	repoDir := cfg.RepoDir
	frontendDir := cfg.FrontendRepoDir
	backendDir := cfg.BackendRepoDir

	// System repo: workflows, system-test, VERSION
	systemWorkflows := []string{
		"multitier-system-" + testLang + "-acceptance-stage.yml",
		"multitier-system-" + testLang + "-qa-stage.yml",
		"multitier-system-" + testLang + "-qa-signoff.yml",
		"multitier-system-" + testLang + "-prod-stage.yml",
	}
	if backendLang == testLang {
		systemWorkflows = append(systemWorkflows, "multitier-system-"+backendLang+"-verify.yml")
	}
	templates.CopyWorkflows(systemWorkflows, starter, repoDir)

	testDst := filepath.Join(repoDir, "system-test", testLang)
	files.CopyDir(filepath.Join(starter, "system-test", testLang), testDst)
	templates.SelectDockerCompose(testDst, "multi")
	templates.CopyVersion(starter, repoDir)

	// Cross-language fixup (before image URL fixup)
	if backendLang != testLang {
		fixupMultitierCrossLangSystem(repoDir, backendLang, testLang)
	}

	// Fix system workflows for multi-repo
	templates.FixupMultirepoImageURLs(repoDir, cfg.Repo, cfg.FrontendRepo, cfg.BackendRepo, backendLang)
	templates.FixupMultirepoToken(repoDir)
	log.OK("Applied system repo template")

	// Backend repo
	backendComponent := "backend-" + backendLang
	backendSrc := filepath.Join(starter, "system", "multitier", backendComponent)
	EnsureWorkflowDir(backendDir)

	entries, _ := os.ReadDir(backendSrc)
	for _, e := range entries {
		src := filepath.Join(backendSrc, e.Name())
		dst := filepath.Join(backendDir, e.Name())
		if e.IsDir() {
			files.CopyDir(src, dst)
		} else {
			files.CopyFile(src, dst)
		}
	}
	templates.CopyWorkflows(
		[]string{"multitier-" + backendComponent + "-commit-stage.yml"},
		starter, backendDir,
	)
	templates.FixupCommitStageForStandalone(backendDir, backendComponent, backendLang)
	log.OK("Applied backend repo template")

	// Frontend repo
	frontendComponent := "frontend-" + frontendLang
	frontendSrc := filepath.Join(starter, "system", "multitier", frontendComponent)
	EnsureWorkflowDir(frontendDir)

	entries, _ = os.ReadDir(frontendSrc)
	for _, e := range entries {
		src := filepath.Join(frontendSrc, e.Name())
		dst := filepath.Join(frontendDir, e.Name())
		if e.IsDir() {
			files.CopyDir(src, dst)
		} else {
			files.CopyFile(src, dst)
		}
	}
	templates.CopyWorkflows(
		[]string{"multitier-" + frontendComponent + "-commit-stage.yml"},
		starter, frontendDir,
	)
	templates.FixupCommitStageForStandalone(frontendDir, frontendComponent, frontendLang)
	log.OK("Applied frontend repo template")
}

// fixupMonolithCrossLang fixes Docker image name and port when system language != test language.
func fixupMonolithCrossLang(repoDir, lang, testLang string) {
	oldImage := "monolith-" + testLang + "-monolith"
	newImage := "monolith-" + lang + "-monolith"

	targets := []string{
		filepath.Join(repoDir, ".github", "workflows", "monolith-"+testLang+"-acceptance-stage.yml"),
		filepath.Join(repoDir, ".github", "workflows", "monolith-"+testLang+"-qa-stage.yml"),
		filepath.Join(repoDir, ".github", "workflows", "monolith-"+testLang+"-prod-stage.yml"),
	}
	for _, prefix := range []string{"local", "pipeline"} {
		for _, suffix := range []string{"real", "stub"} {
			targets = append(targets, filepath.Join(repoDir, "system-test", testLang,
				"docker-compose."+prefix+".monolith."+suffix+".yml"))
		}
	}
	for _, path := range targets {
		if _, err := os.Stat(path); err == nil {
			files.ReplaceInFile(path, oldImage, newImage)
		}
	}

	// Fix port mapping
	systemPort := internalPorts[lang]
	templatePort := internalPorts[testLang]
	if systemPort != templatePort {
		for _, prefix := range []string{"local", "pipeline"} {
			for _, suffix := range []string{"real", "stub"} {
				compose := filepath.Join(repoDir, "system-test", testLang,
					"docker-compose."+prefix+".monolith."+suffix+".yml")
				if _, err := os.Stat(compose); err == nil {
					files.ReplaceInFile(compose,
						"8080:"+itoa(templatePort),
						"8080:"+itoa(systemPort))
				}
			}
		}
	}

	log.OKf("Cross-language fixup: %s -> %s", oldImage, newImage)
}

// fixupMultitierCrossLangSystem fixes Docker backend image name in system repo.
func fixupMultitierCrossLangSystem(repoDir, backendLang, testLang string) {
	oldImage := "multitier-backend-" + testLang
	newImage := "multitier-backend-" + backendLang

	targets := []string{
		filepath.Join(repoDir, ".github", "workflows", "multitier-system-"+testLang+"-acceptance-stage.yml"),
		filepath.Join(repoDir, ".github", "workflows", "multitier-system-"+testLang+"-qa-stage.yml"),
		filepath.Join(repoDir, ".github", "workflows", "multitier-system-"+testLang+"-prod-stage.yml"),
	}
	for _, prefix := range []string{"local", "pipeline"} {
		for _, suffix := range []string{"real", "stub"} {
			targets = append(targets, filepath.Join(repoDir, "system-test", testLang,
				"docker-compose."+prefix+".multitier."+suffix+".yml"))
		}
	}
	for _, path := range targets {
		if _, err := os.Stat(path); err == nil {
			files.ReplaceInFile(path, oldImage, newImage)
		}
	}

	log.OKf("Cross-language fixup: %s -> %s", oldImage, newImage)
}

func itoa(n int) string {
	return fmt.Sprintf("%d", n)
}
