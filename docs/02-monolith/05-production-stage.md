# Monolith - Production Stage

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Verify the Production Stage

1. Trigger the production stage with the RC version that passed QA signoff (CLI):
   ```bash
   gh workflow run prod-stage.yml --repo <owner>/<repo> -f version=<rc-version>
   ```
2. Wait for completion (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
   If it fails, stop and ask for support.
3. Verify the final release exists and is marked Latest (CLI):
   ```bash
   gh release list --repo <owner>/<repo> --limit 5
   ```
   You should see a release with the `-rc` suffix removed (e.g. `v0.0.1`) marked as **Latest**.

## Checklist

1. `prod-stage` workflow completes successfully
2. Release is tagged and marked as Latest in GitHub Releases
3. Monolith Package has final version tag (e.g. `-rc` suffix removed)
