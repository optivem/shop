# Monolith - Acceptance Stage

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Verify the Acceptance Stage

1. Trigger the acceptance stage (CLI):
   ```bash
   gh workflow run acceptance-stage.yml --repo <owner>/<repo>
   ```
2. Wait for completion (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
   If it fails, stop and ask for support.
3. Verify a prerelease candidate was created (CLI):
   ```bash
   gh release list --repo <owner>/<repo> --limit 5
   ```
   You should see a version like `v0.0.1-rc`.
4. Verify the Docker image has the RC tag (CLI):
   ```bash
   gh api users/<owner>/packages/container/monolith/versions --jq '.[0].metadata.container.tags'
   ```

*The Acceptance Stage is a scheduled workflow. You don't normally trigger it manually — we triggered it above to verify your setup without waiting for the scheduled run.*

*Since your repository is public, GitHub Actions usage is free (for standard GitHub-hosted runners) and you don't need to worry about minutes cost.*

## Checklist

1. `acceptance-stage` workflow completes successfully
2. RC version is created (e.g. `v0.0.1-rc`) in GitHub Releases
3. Docker image is tagged with the RC version in Packages
