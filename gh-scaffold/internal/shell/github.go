// Package shell provides GitHub CLI wrapper and subprocess helpers.
package shell

import (
	"encoding/json"
	"fmt"
	"os/exec"
	"strings"
	"time"

	"github.com/optivem/gh-scaffold/internal/config"
	"github.com/optivem/gh-scaffold/internal/log"
)

const rateLimitThreshold = 50

// RateLimitExceeded is returned when a gh command fails due to rate limiting.
type RateLimitExceeded struct {
	Msg string
}

func (e *RateLimitExceeded) Error() string { return e.Msg }

// Run executes a shell command. In dry-run mode, just prints it.
func Run(cmdStr string, dryRun bool, check bool, cwd string) (string, error) {
	if dryRun {
		log.Logf("[DRY RUN] %s", cmdStr)
		return "", nil
	}

	parts := splitCommand(cmdStr)
	cmd := exec.Command(parts[0], parts[1:]...)
	if cwd != "" {
		cmd.Dir = cwd
	}

	out, err := cmd.CombinedOutput()
	output := string(out)

	if err != nil {
		lower := strings.ToLower(output)
		if strings.Contains(lower, "rate limit") || strings.Contains(lower, "api rate limit exceeded") {
			return output, &RateLimitExceeded{Msg: fmt.Sprintf("GitHub API rate limit exceeded. Command: %s\n%s", cmdStr, output)}
		}
		if check {
			return output, fmt.Errorf("command failed: %s\n%s", cmdStr, output)
		}
	}
	return output, nil
}

// RunCapture runs a command and captures stdout separately.
func RunCapture(cmdStr string, cwd string) (string, error) {
	parts := splitCommand(cmdStr)
	cmd := exec.Command(parts[0], parts[1:]...)
	if cwd != "" {
		cmd.Dir = cwd
	}
	out, err := cmd.Output()
	return strings.TrimSpace(string(out)), err
}

// RunPassthrough runs a command with stdout/stderr passed through to the terminal.
func RunPassthrough(cmdStr string, cwd string) error {
	parts := splitCommand(cmdStr)
	cmd := exec.Command(parts[0], parts[1:]...)
	if cwd != "" {
		cmd.Dir = cwd
	}
	cmd.Stdout = nil // inherit
	cmd.Stderr = nil // inherit
	return cmd.Run()
}

// splitCommand splits a command string into parts, respecting quotes.
func splitCommand(s string) []string {
	var parts []string
	var current strings.Builder
	inQuote := false
	quoteChar := byte(0)

	for i := 0; i < len(s); i++ {
		c := s[i]
		if inQuote {
			if c == quoteChar {
				inQuote = false
			} else {
				current.WriteByte(c)
			}
		} else if c == '"' || c == '\'' {
			inQuote = true
			quoteChar = c
		} else if c == ' ' || c == '\t' {
			if current.Len() > 0 {
				parts = append(parts, current.String())
				current.Reset()
			}
		} else {
			current.WriteByte(c)
		}
	}
	if current.Len() > 0 {
		parts = append(parts, current.String())
	}
	return parts
}

// CheckRateLimit checks the GitHub API rate limit and waits if low.
func CheckRateLimit() {
	out, err := RunCapture("gh api rate_limit --jq .resources.core", "")
	if err != nil {
		return
	}

	var data struct {
		Remaining int   `json:"remaining"`
		Reset     int64 `json:"reset"`
	}
	if json.Unmarshal([]byte(out), &data) != nil {
		return
	}

	if data.Remaining < rateLimitThreshold {
		waitSecs := data.Reset - time.Now().Unix() + 5
		if waitSecs > 0 {
			log.Logf("Rate limit low (%d remaining). Waiting %ds for reset...", data.Remaining, waitSecs)
			time.Sleep(time.Duration(waitSecs) * time.Second)
		} else {
			log.Logf("Rate limit low (%d remaining) but reset is imminent.", data.Remaining)
		}
	}
}

// GitHub wraps gh CLI calls for a specific repo.
type GitHub struct {
	Repo   string
	DryRun bool
}

func NewGitHub(cfg *config.Config) *GitHub {
	return &GitHub{Repo: cfg.FullRepo, DryRun: cfg.DryRun}
}

func (g *GitHub) ForRepo(fullRepo string) *GitHub {
	return &GitHub{Repo: fullRepo, DryRun: g.DryRun}
}

func (g *GitHub) run(cmd string) (string, error) {
	return Run(fmt.Sprintf("gh %s --repo %s", cmd, g.Repo), g.DryRun, true, "")
}

func (g *GitHub) CreateRepo() {
	out, err := RunCapture(fmt.Sprintf("gh repo view %s --json name", g.Repo), "")
	if err == nil && out != "" {
		log.Warnf("Repository %s already exists -- skipping creation", g.Repo)
		return
	}
	Run(fmt.Sprintf("gh repo create %s --public --add-readme --license mit", g.Repo), false, true, "")
}

func (g *GitHub) CreateEnvironment(name string) {
	Run(fmt.Sprintf("gh api repos/%s/environments/%s -X PUT", g.Repo, name), g.DryRun, true, "")
}

func (g *GitHub) SecretSet(name, value string) {
	if g.DryRun {
		log.Logf("[DRY RUN] gh secret set %s --body *** --repo %s", name, g.Repo)
		return
	}
	Run(fmt.Sprintf("gh secret set %s --body %s --repo %s", name, value, g.Repo), false, true, "")
}

func (g *GitHub) VariableSet(name, value string) {
	if g.DryRun {
		log.Logf("[DRY RUN] gh variable set %s --body \"%s\" --repo %s", name, value, g.Repo)
		return
	}
	Run(fmt.Sprintf("gh variable set %s --body %s --repo %s", name, value, g.Repo), false, true, "")
}

func (g *GitHub) Clone(dest string) {
	Run(fmt.Sprintf("gh repo clone %s %s", g.Repo, dest), false, true, "")
}

func (g *GitHub) WorkflowRun(workflow string, fields map[string]string) {
	var fieldArgs string
	for k, v := range fields {
		fieldArgs += fmt.Sprintf(" -f %s=%s", k, v)
	}
	g.run(fmt.Sprintf("workflow run %s%s", workflow, fieldArgs))
}

func (g *GitHub) RunWatch() error {
	// Get latest run ID
	out, err := RunCapture(
		fmt.Sprintf("gh run list --repo %s --limit 1 --json databaseId --jq .[0].databaseId", g.Repo), "")
	if err != nil || out == "" {
		return fmt.Errorf("no workflow runs found for %s", g.Repo)
	}
	_, err = Run(fmt.Sprintf("gh run watch %s --repo %s --exit-status", out, g.Repo), false, false, "")
	return err
}

func (g *GitHub) Delete() {
	Run(fmt.Sprintf("gh repo delete %s --yes", g.Repo), false, false, "")
}
