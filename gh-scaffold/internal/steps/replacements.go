package steps

import (
	"os"
	"path/filepath"
	"strings"

	"github.com/optivem/gh-scaffold/internal/config"
	"github.com/optivem/gh-scaffold/internal/files"
	"github.com/optivem/gh-scaffold/internal/log"
	"github.com/optivem/gh-scaffold/internal/templates"
)

// All text file extensions to process.
var textExts = []string{
	".yml", ".yaml", ".md", ".gradle", ".gradle.kts",
	".csproj", ".sln", ".slnx", ".cshtml", ".json",
	".cs", ".java", ".ts", ".tsx", ".js", ".jsx",
	".xml", ".properties", ".cfg", ".txt",
}

// ReplaceRepoReferences replaces optivem/starter references with the target repo.
func ReplaceRepoReferences(cfg *config.Config) {
	log.Log("Step 6: Replacing repository references...")

	if cfg.DryRun {
		log.Logf("[DRY RUN] Would replace optivem/starter -> %s", cfg.FullRepo)
		return
	}

	if cfg.Arch == "monolith" {
		replaceRefsInRepo(cfg.RepoDir, cfg.FullRepo, cfg.OwnerLower)
	} else {
		replaceRefsInRepo(cfg.RepoDir, cfg.FullRepo, cfg.OwnerLower)
		replaceRefsInRepo(cfg.BackendRepoDir, cfg.BackendFullRepo, cfg.OwnerLower)
		replaceRefsInRepo(cfg.FrontendRepoDir, cfg.FrontendFullRepo, cfg.OwnerLower)

		// Fix docker-compose image URLs
		templates.FixupMultirepoDockerCompose(
			cfg.RepoDir, cfg.Repo, cfg.FrontendRepo, cfg.BackendRepo, cfg.BackendLang,
		)
	}

	log.OK("Repository reference replacement complete")
}

func replaceRefsInRepo(repoDir, fullRepo, ownerLower string) {
	// Pass 1: optivem/starter -> owner/repo
	n := files.ReplaceInTree(repoDir, "optivem/starter", fullRepo, textExts)
	n += files.ReplaceInDockerfiles(repoDir, "optivem/starter", fullRepo)
	log.OKf("Pass 1: replaced optivem/starter -> %s (%d files)", fullRepo, n)

	// Pass 2: optivem_starter -> owner_repo (SonarCloud underscore variant)
	underscoreNew := strings.ReplaceAll(fullRepo, "/", "_")
	n = files.ReplaceInTree(repoDir, "optivem_starter", underscoreNew, textExts)
	log.OKf("Pass 2: replaced optivem_starter -> %s (%d files)", underscoreNew, n)

	// Pass 3: SonarCloud org patterns
	sonarReplacements := [][2]string{
		{"'sonar.organization', 'optivem'", "'sonar.organization', '" + ownerLower + "'"},
		{`/o:"optivem"`, `/o:"` + ownerLower + `"`},
		{"-Dsonar.organization=optivem", "-Dsonar.organization=" + ownerLower},
	}
	for _, pair := range sonarReplacements {
		n = files.ReplaceInTree(repoDir, pair[0], pair[1], nil)
		if n > 0 {
			log.OKf("Pass 3: replaced sonar org pattern (%d files)", n)
		}
	}

	// Safety check: optivem/actions must still be intact
	wfDir := filepath.Join(repoDir, ".github", "workflows")
	if info, err := os.Stat(wfDir); err == nil && info.IsDir() {
		actionsFound := false
		entries, _ := os.ReadDir(wfDir)
		for _, e := range entries {
			if !strings.HasSuffix(e.Name(), ".yml") {
				continue
			}
			data, err := os.ReadFile(filepath.Join(wfDir, e.Name()))
			if err != nil {
				continue
			}
			if strings.Contains(string(data), "optivem/actions") {
				actionsFound = true
				break
			}
		}
		if !actionsFound {
			log.Fatalf("Safety check failed: optivem/actions references were corrupted in %s!", repoDir)
		}
		log.OKf("Safety check passed: optivem/actions references intact in %s", repoDir)
	}

	lowercaseDockerComposeImages(repoDir)
}

func lowercaseDockerComposeImages(repoDir string) {
	filepath.Walk(repoDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() {
			return nil
		}
		if files.IsGitDir(path) {
			return nil
		}
		if !strings.Contains(info.Name(), "docker-compose") || !strings.HasSuffix(info.Name(), ".yml") {
			return nil
		}

		data, readErr := os.ReadFile(path)
		if readErr != nil {
			return nil
		}

		lines := strings.Split(string(data), "\n")
		changed := false
		for i, line := range lines {
			if strings.Contains(line, "image:") && strings.Contains(line, "ghcr.io") {
				idx := strings.Index(line, "image:")
				prefix := line[:idx+6]
				rest := line[idx+6:]
				lowered := prefix + strings.ToLower(rest)
				if lowered != lines[i] {
					lines[i] = lowered
					changed = true
				}
			}
		}
		if changed {
			os.WriteFile(path, []byte(strings.Join(lines, "\n")), 0644)
		}
		return nil
	})
	log.OK("Docker-compose image URLs lowercased")
}

// ReplaceNamespaces replaces language-specific namespaces.
func ReplaceNamespaces(cfg *config.Config) {
	log.Log("Step 7: Replacing namespaces...")

	if cfg.DryRun {
		log.Log("[DRY RUN] Would replace language-specific namespaces")
		return
	}

	if cfg.Arch == "monolith" {
		nsForLang(cfg, cfg.Lang, "monolith", cfg.RepoDir)
		nsForLang(cfg, cfg.TestLang, "systemtest", cfg.RepoDir)
	} else {
		// System repo: only system-test namespaces
		nsForLang(cfg, cfg.TestLang, "systemtest", cfg.RepoDir)
		// Backend repo
		nsForLang(cfg, cfg.BackendLang, "backend", cfg.BackendRepoDir)
		// Frontend repo
		if cfg.FrontendLang == "react" {
			fixupFrontendPackageJSON(cfg)
		}
	}

	log.OK("Namespace replacement complete")
}

func nsForLang(cfg *config.Config, lang, component, repoDir string) {
	switch lang {
	case "java":
		nsJava(cfg, component, repoDir)
	case "dotnet":
		nsDotnet(cfg, component, repoDir)
	case "typescript":
		nsTypeScript(cfg, component, repoDir)
	}
}

func nsJava(cfg *config.Config, component, repoDir string) {
	oldFull := cfg.JavaNsOld + "." + component
	newFull := cfg.JavaNsNew + "." + component

	n := files.ReplaceInTree(repoDir, oldFull, newFull, []string{".java", ".gradle", ".gradle.kts", ".xml", ".properties"})
	n += files.ReplaceInTree(repoDir, oldFull, newFull, []string{".yml"})
	log.OKf("Java: replaced %s -> %s (%d files)", oldFull, newFull, n)

	oldDirParts := []string{"com", "optivem", "starter"}
	newDirParts := []string{"com", cfg.OwnerLower, cfg.RepoNoHyphens}

	filepath.Walk(repoDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || !info.IsDir() || files.IsGitDir(path) {
			return nil
		}
		check := filepath.Join(path, "com", "optivem", "starter")
		if _, err := os.Stat(check); err == nil {
			files.RenameJavaDirs(path, oldDirParts, newDirParts)
			return filepath.SkipDir
		}
		return nil
	})
	log.OKf("Java: renamed directories com/optivem/starter -> com/%s/%s", cfg.OwnerLower, cfg.RepoNoHyphens)
}

func nsDotnet(cfg *config.Config, component, repoDir string) {
	componentMap := map[string]string{
		"monolith": "Monolith", "backend": "Backend", "systemtest": "SystemTest",
	}
	oldFull := cfg.DotnetNsOld + "." + componentMap[component]
	newFull := cfg.DotnetNsNew + "." + componentMap[component]

	n := files.ReplaceInTree(repoDir, oldFull, newFull, []string{".cs", ".cshtml", ".csproj", ".sln", ".slnx", ".json", ".yml"})
	n += files.ReplaceInDockerfiles(repoDir, oldFull, newFull)
	log.OKf(".NET: replaced %s -> %s (%d files)", oldFull, newFull, n)

	files.RenameDotnetFiles(repoDir, oldFull, newFull)
	log.OKf(".NET: renamed files %s.* -> %s.*", oldFull, newFull)
}

func nsTypeScript(cfg *config.Config, component, repoDir string) {
	if component != "systemtest" {
		return
	}

	n := files.ReplaceInTree(repoDir, cfg.TsPkgOld, cfg.TsPkgNew, []string{".json"})
	log.OKf("TypeScript: replaced %s -> %s (%d files)", cfg.TsPkgOld, cfg.TsPkgNew, n)

	// Update package.json metadata in system-test
	filepath.Walk(repoDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() {
			return nil
		}
		if strings.Contains(path, "system-test") && info.Name() == "package.json" {
			files.ReplaceInFile(path, `"author": "Optivem"`, `"author": "`+cfg.Owner+`"`)
			files.ReplaceInFile(path, `"Starter - System Tests"`, `"`+cfg.SystemName+` - System Tests"`)
			files.ReplaceInFile(path, `"optivem"`, `"`+cfg.OwnerLower+`"`)
			log.OK("TypeScript: updated package.json metadata")
			return filepath.SkipAll
		}
		return nil
	})

	// Update package.json in system dirs (monolith/backend)
	filepath.Walk(repoDir, func(path string, info os.FileInfo, err error) error {
		if err != nil || info.IsDir() || files.IsGitDir(path) {
			return nil
		}
		if strings.Contains(path, "system-test") || strings.Contains(path, "node_modules") {
			return nil
		}
		if info.Name() == "package.json" {
			if strings.Contains(path, "monolith") {
				files.ReplaceInFile(path, `"name": "starter-monolith"`, `"name": "`+cfg.Repo+`-monolith"`)
			} else if strings.Contains(path, "backend") {
				files.ReplaceInFile(path, `"name": "starter-backend"`, `"name": "`+cfg.Repo+`-backend"`)
			}
		}
		return nil
	})
}

func fixupFrontendPackageJSON(cfg *config.Config) {
	pkgPath := filepath.Join(cfg.FrontendRepoDir, "package.json")
	if _, err := os.Stat(pkgPath); err == nil {
		files.ReplaceInFile(pkgPath, `"name": "starter-frontend"`, `"name": "`+cfg.Repo+`-frontend"`)
		files.ReplaceInFile(pkgPath, `"name": "frontend-react"`, `"name": "`+cfg.Repo+`-frontend"`)
	}
}
