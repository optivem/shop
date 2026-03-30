# QA Stage - Multirepo

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Update Image References

Open the file `qa-stage.yml`.

In the job `resolve-docker-images`, find the input `base-image-urls`. For each component, replace `${{ github.event.repository.name }}` with the corresponding component repository name.

Example:

```
base-image-urls: |
  ghcr.io/${{ github.repository_owner }}/eshop-frontend/frontend
  ghcr.io/${{ github.repository_owner }}/eshop-backend/backend
```

Commit and push (CLI):

```bash
git add -A && git commit -m "Update QA stage for multirepo" && git push
```

Trigger the QA Stage (CLI):

```bash
gh workflow run qa-stage.yml --repo <owner>/<repo> -f version=<rc-version>
gh run watch --repo <owner>/<repo>
```

Verify that the QA Stage passes.

Trigger QA Signoff (CLI):

```bash
gh workflow run qa-signoff.yml --repo <owner>/<repo> -f version=<rc-version> -f result=approved
gh run watch --repo <owner>/<repo>
```

Verify that QA Signoff passes.

> **Note:** After updating the QA Stage, the Production Stage may still fail because it still references the old image URLs. This is expected — you will update it in the Production Stage multirepo section. For now, only verify that the QA Stage passes.

## Checklist

1. QA Stage references correct image URLs from component repositories
2. Cross-repository tagging works with `DOCKER_REGISTRY_TOKEN`
3. `qa-stage` workflow completes successfully
4. `qa-signoff` workflow completes successfully
