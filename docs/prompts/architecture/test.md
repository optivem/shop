# Test File Rules

## Positive vs Negative Test Classes

Each use case has two test files (see `language-equivalents.md` for the file extension per language):

- **`<UseCase>PositiveTest`** — scenarios where `Then` asserts **success** (e.g. `shouldSucceed()`, resource is returned, state is correct).
- **`<UseCase>NegativeTest`** — scenarios where `Then` asserts **failure** (e.g. `shouldFailWith(...)`, error message returned).

When writing a first scenario and leaving the rest as `// TODO:` comments:
- If the first scenario is positive, put its `// TODO:` siblings in the **positive** file only if they are also positive; put negative `// TODO:` lines in the **negative** file.
- If new DSL is needed and only one test method is written, the remaining `// TODO:` lines must go into the correct file based on this rule — never mix positive and negative TODOs in the same file.
