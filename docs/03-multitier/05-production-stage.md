# Production Stage - Multitier

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Update Image References

Open the file `prod-stage.yml`.

Find the word `monolith` inside `base-image-urls` — there's one line. Copy-paste that line so you have two lines. In the first line, replace `monolith` with `frontend`. In the second line, replace `monolith` with `backend`.

Commit and push (CLI):

```bash
git add -A && git commit -m "Update production stage for multitier" && git push
```

Trigger the PROD Stage (CLI):

```bash
gh workflow run prod-stage.yml --repo <owner>/<repo> -f version=<rc-version>
gh run watch --repo <owner>/<repo>
```

Verify that PROD Stage passes.

## Checklist

1. Production Stage deploys all component artifacts
2. Final system version tag applied to all component artifacts
3. `prod-stage` workflow completes successfully
