// Package templates provides template helpers: copy workflows, docker-compose selection, fixups.
package templates

import (
	"os"
	"path/filepath"
	"strings"

	"github.com/optivem/gh-scaffold/internal/files"
	"github.com/optivem/gh-scaffold/internal/log"
)

// CopyWorkflows copies workflow files from starter to repo.
func CopyWorkflows(workflowNames []string, starter, repoDir string) {
	wfSrc := filepath.Join(starter, ".github", "workflows")
	wfDst := filepath.Join(repoDir, ".github", "workflows")
	os.MkdirAll(wfDst, 0755)

	for _, wf := range workflowNames {
		src := filepath.Join(wfSrc, wf)
		if _, err := os.Stat(src); err != nil {
			log.Warnf("Workflow not found: %s", wf)
			continue
		}
		files.CopyFile(src, filepath.Join(wfDst, wf))
	}
}

// SelectDockerCompose keeps the chosen variant and removes the other.
// variant: "single" for monolith, "multi" for multitier.
func SelectDockerCompose(testDst, variant string) {
	remove := "multitier"
	if variant != "single" {
		remove = "monolith"
	}
	for _, prefix := range []string{"local", "pipeline"} {
		for _, suffix := range []string{"real", "stub"} {
			path := filepath.Join(testDst, "docker-compose."+prefix+"."+remove+"."+suffix+".yml")
			os.Remove(path)
		}
	}
}

// CopyVersion copies the VERSION file from starter to repo.
func CopyVersion(starter, repoDir string) {
	src := filepath.Join(starter, "VERSION")
	if _, err := os.Stat(src); err == nil {
		files.CopyFile(src, filepath.Join(repoDir, "VERSION"))
	}
}

// FixupMultirepoImageURLs replaces image URLs in system workflows for multi-repo setup.
func FixupMultirepoImageURLs(repoDir, repoName, frontendRepo, backendRepo, backendLang string) {
	wfDir := filepath.Join(repoDir, ".github", "workflows")
	if _, err := os.Stat(wfDir); err != nil {
		return
	}

	oldFrontend := "${{ github.event.repository.name }}/multitier-frontend-react"
	newFrontend := frontendRepo + "/multitier-frontend-react"
	oldBackend := "${{ github.event.repository.name }}/multitier-backend-" + backendLang
	newBackend := backendRepo + "/multitier-backend-" + backendLang

	entries, _ := os.ReadDir(wfDir)
	for _, e := range entries {
		if !strings.HasSuffix(e.Name(), ".yml") || !strings.Contains(e.Name(), "system") {
			continue
		}
		path := filepath.Join(wfDir, e.Name())
		files.ReplaceInFile(path, oldFrontend, newFrontend)
		files.ReplaceInFile(path, oldBackend, newBackend)
	}
}

// FixupMultirepoToken replaces GITHUB_TOKEN with GHCR_TOKEN in acceptance/prod stage workflows.
func FixupMultirepoToken(repoDir string) {
	wfDir := filepath.Join(repoDir, ".github", "workflows")
	if _, err := os.Stat(wfDir); err != nil {
		return
	}

	entries, _ := os.ReadDir(wfDir)
	for _, e := range entries {
		name := e.Name()
		if !strings.HasSuffix(name, ".yml") {
			continue
		}
		if !strings.Contains(name, "acceptance-stage") && !strings.Contains(name, "prod-stage") {
			continue
		}
		path := filepath.Join(wfDir, name)
		files.ReplaceInFile(path,
			"GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}",
			"GITHUB_TOKEN: ${{ secrets.GHCR_TOKEN }}")
	}
}

// FixupMultirepoDockerCompose replaces system repo name with component repo names in docker-compose.
func FixupMultirepoDockerCompose(repoDir, repoName, frontendRepo, backendRepo, backendLang string) {
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
		files.ReplaceInFile(path,
			repoName+"/multitier-frontend-react",
			frontendRepo+"/multitier-frontend-react")
		files.ReplaceInFile(path,
			repoName+"/multitier-backend-"+backendLang,
			backendRepo+"/multitier-backend-"+backendLang)
		return nil
	})
}

// FixupCommitStageForStandalone adapts a commit stage workflow for a standalone component repo.
func FixupCommitStageForStandalone(repoDir, component, lang string) {
	wfDir := filepath.Join(repoDir, ".github", "workflows")
	if _, err := os.Stat(wfDir); err != nil {
		return
	}

	oldPathPrefix := "system/multitier/" + component

	entries, _ := os.ReadDir(wfDir)
	for _, e := range entries {
		if !strings.HasSuffix(e.Name(), ".yml") || !strings.Contains(e.Name(), "commit-stage") {
			continue
		}
		path := filepath.Join(wfDir, e.Name())
		data, err := os.ReadFile(path)
		if err != nil {
			continue
		}

		lines := strings.Split(string(data), "\n")
		var newLines []string
		i := 0
		for i < len(lines) {
			line := lines[i]

			// Remove path filter blocks
			if strings.TrimSpace(line) == "paths:" {
				i++
				for i < len(lines) && strings.HasPrefix(strings.TrimSpace(lines[i]), "- '") {
					i++
				}
				continue
			}

			// Replace working-directory references
			if strings.Contains(line, "working-directory: "+oldPathPrefix) {
				line = strings.Replace(line, "working-directory: "+oldPathPrefix, "working-directory: .", 1)
			}
			// Replace VERSION file references
			if strings.Contains(line, "file: "+oldPathPrefix+"/VERSION") {
				line = strings.Replace(line, "file: "+oldPathPrefix+"/VERSION", "file: VERSION", 1)
			}
			// Replace SonarCloud projectBaseDir
			if strings.Contains(line, "projectBaseDir: "+oldPathPrefix) {
				line = strings.Replace(line, "projectBaseDir: "+oldPathPrefix, "projectBaseDir: .", 1)
			}

			newLines = append(newLines, line)
			i++
		}

		os.WriteFile(path, []byte(strings.Join(newLines, "\n")), 0644)
	}
}
