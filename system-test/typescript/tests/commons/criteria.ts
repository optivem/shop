import type { TestDetails } from '@playwright/test';

// Links a test to the use-case acceptance criteria it covers (for example
// UC-001.3, see docs/shared/specs/use-cases/). Each ID becomes a `tag` annotation, which
// Playwright's JUnit reporter writes into the <testcase> as
// <property name="tag" value="UC-001.3"/>: the same shape the Java build emits
// for @Tag("UC-001.3"), and what the `gh optivem spec` gate reads.
// Each ID is also a native Playwright tag (@UC-001.3), so `--grep` can select
// a use case's tests: `gh optivem system-test run --use-case UC-001` relies on it.
export function criteria(...ids: string[]): TestDetails {
    return {
        tag: ids.map((id) => `@${id}`),
        annotation: ids.map((id) => ({ type: 'tag', description: id })),
    };
}
