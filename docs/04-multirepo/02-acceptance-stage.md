# Acceptance Stage - Multirepo

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## 1. Create Token

Create a Personal Access Token (classic) with `write:packages` and `read:packages` scopes (browser — token creation cannot be done via CLI):

1. Go to https://github.com/settings/tokens and click **Generate new token** → **Generate new token (classic)**.
2. In Note, write: `GHCR_TOKEN`
3. Under Select scopes, tick: `write:packages`, `read:packages`.
4. Click **Generate token** and copy the token value.

Add the PAT as a secret on your system repository (CLI):

```bash
gh secret set GHCR_TOKEN --repo <owner>/<system-repo>
```

Paste the token value when prompted.

## 2. Update Image References

Open `acceptance-stage.yml`.

In the job `find-latest-images`, find the input `image-urls`. For each component, replace `${{ github.event.repository.name }}` with the corresponding component repository name.

Example:

```
image-urls: |
  ghcr.io/${{ github.repository_owner }}/eshop-frontend/frontend:latest
  ghcr.io/${{ github.repository_owner }}/eshop-backend/backend:latest
```

## 3. Update RC Tagging

In the step `Tag Docker Images for Prerelease`, set the value `GITHUB_TOKEN: ${{ secrets.GHCR_TOKEN }}`.

## 4. Update Docker Compose

Update `system-test/${TEST_LANG}/docker-compose.monolith.yml` to reference the component images from their respective repositories.

## 5. Verify

Commit and push (CLI):

```bash
git add -A && git commit -m "Update acceptance stage for multirepo" && git push
```

Trigger `acceptance-stage` (CLI):

```bash
gh workflow run acceptance-stage.yml --repo <owner>/<repo>
gh run watch --repo <owner>/<repo>
```

Verify that it is successful.

## Checklist

1. `GHCR_TOKEN` secret is set
2. Acceptance Stage finds latest artifacts from each component's repository
3. Docker Compose references correct cross-repository image URLs
4. Cross-repository RC tagging works with `GHCR_TOKEN`
5. `acceptance-stage` workflow completes successfully
