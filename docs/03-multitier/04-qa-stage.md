# QA Stage - Multitier

For a working example, see the [Starter](https://github.com/optivem/starter) template.

## Update Image References

Open the file `qa-stage.yml`.

Find the word `monolith` inside `base-image-urls` — there's one line. Copy-paste that line so you have two lines. In the first line, replace `monolith` with `frontend`. In the second line, replace `monolith` with `backend`.

Commit and push (CLI):

```bash
git add -A && git commit -m "Update QA stage for multitier" && git push
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

> **Note:** After updating the QA Stage, the Production Stage may still fail because it still references the old image URLs. This is expected — you will update it in the Production Stage multitier section. For now, only verify that the QA Stage passes.

## Checklist

1. QA Stage deploys all component artifacts
2. System RC version tags applied to all component artifacts
3. `qa-stage` workflow completes successfully
4. `qa-signoff` workflow completes successfully
