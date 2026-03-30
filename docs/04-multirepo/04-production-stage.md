# Production Stage - Multirepo

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Update Image References

Open the file `prod-stage.yml`.

In the job `resolve-docker-images`, find the input `base-image-urls`. For each component, replace `${{ github.event.repository.name }}` with the corresponding component repository name.

In the step `Tag Docker Images for Production`, set the value `GITHUB_TOKEN: ${{ secrets.DOCKER_REGISTRY_TOKEN }}`.

Commit and push (CLI):

```bash
git add -A && git commit -m "Update production stage for multirepo" && git push
```

Trigger the PROD Stage (CLI):

```bash
gh workflow run prod-stage.yml --repo <owner>/<repo> -f version=<rc-version>
gh run watch --repo <owner>/<repo>
```

Verify that PROD Stage passes.

## Checklist

1. Production Stage references correct image URLs from component repositories
2. Cross-repository final version tagging works with `DOCKER_REGISTRY_TOKEN`
3. `prod-stage` workflow completes successfully
