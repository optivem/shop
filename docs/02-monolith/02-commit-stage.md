# Monolith - Commit Stage

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Verify the Commit Stage

The commit stage should have triggered automatically from the push in the [Apply Template](../01-general/04-apply-template.md) step.

1. If it hasn't completed yet, wait for it (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
2. Verify the Docker image artifact exists in Packages (CLI):
   ```bash
   gh api users/<owner>/packages?package_type=container --jq '.[].name'
   ```
   You should see a `monolith` package.

## Verify SonarCloud Analysis

If you set up SonarCloud during the [Apply Template](../01-general/04-apply-template.md) step, verify the analysis ran:

```bash
SONAR_PROJECT="<project-key-from-build-config>"

curl -s -u "${SONAR_TOKEN}:" \
  "https://sonarcloud.io/api/measures/component?component=${SONAR_PROJECT}&metricKeys=bugs,vulnerabilities,code_smells,coverage,duplicated_lines_density" | jq '.component.measures'
```

You should see metrics for: bugs, vulnerabilities, code smells, coverage, and duplicated lines.

## Checklist

1. Commit stage triggers automatically on push
2. Workflow passes with valid code
3. Docker image artifact is published to Packages
4. SonarCloud analysis runs and metrics are visible
