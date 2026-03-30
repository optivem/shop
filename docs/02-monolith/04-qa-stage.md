# Monolith - QA Stage

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Verify the QA Stage

1. Find the RC version (CLI):
   ```bash
   gh release list --repo <owner>/<repo> --limit 5
   ```
2. Trigger the QA stage with the RC version (CLI):
   ```bash
   gh workflow run qa-stage.yml --repo <owner>/<repo> -f version=<rc-version>
   ```
3. Wait for completion (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
   If it fails, stop and ask for support.
4. Verify the QA deployed release exists (CLI):
   ```bash
   gh release list --repo <owner>/<repo> --limit 5
   ```
   You should see a release with the suffix `-qa-deployed` (e.g. `v0.0.1-rc-qa-deployed`).

## Verify QA Signoff

After deployment, the QA Engineer does manual testing (exploratory testing, usability). Once complete:

1. Trigger the QA signoff (CLI):
   ```bash
   gh workflow run qa-signoff.yml --repo <owner>/<repo> -f version=<rc-version> -f result=approved
   ```
2. Wait for completion (CLI):
   ```bash
   gh run watch --repo <owner>/<repo>
   ```
3. Verify the QA approved release exists (CLI):
   ```bash
   gh release list --repo <owner>/<repo> --limit 5
   ```
   You should see a prerelease with the suffix `-rc-qa-approved`.

## Checklist

1. `qa-stage` workflow completes successfully
2. Release is marked as QA deployed (e.g. `-rc-qa-deployed`)
3. `qa-signoff` workflow completes successfully
4. Release is marked as QA approved (e.g. `-rc-qa-approved`)
