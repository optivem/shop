// Typed facade: the library declares the test callback's argument as `any`; here fixtures and row shape flow through.
import { bindTestEach as bindUntypedTestEach } from '@optivem/optivem-testing';
import type { TestDetails, TestType } from '@playwright/test';

type IsWordChar<C extends string> = C extends '_' | `${number}`
    ? true
    : Lowercase<C> extends Uppercase<C>
      ? false
      : true;

type PlaceholderKey<S extends string, Key extends string = ''> = S extends `${infer C}${infer Rest}`
    ? IsWordChar<C> extends true
        ? PlaceholderKey<Rest, `${Key}${C}`>
        : Key
    : Key;

type Placeholders<Name extends string> = Name extends `${string}$${infer Rest}`
    ? PlaceholderKey<Rest> | Placeholders<Rest>
    : never;

type RowFixtures<TCase, TName extends string> = [TCase] extends [object]
    ? TCase
    : [Placeholders<TName>] extends [never]
      ? { value: TCase }
      : Record<Placeholders<TName>, TCase>;

type RowTestFn<TArgs, TCase, TName extends string> = (args: TArgs & RowFixtures<TCase, TName>) => Promise<void>;

// Optional `details` (for example criteria('UC-001.2')) is applied to every row's
// test, just as in Playwright's own test(title, details, body).
export type TestEach<TArgs> = <TCase>(cases: readonly TCase[]) => {
    <TName extends string>(name: TName, fn: RowTestFn<TArgs, TCase, TName>): void;
    <TName extends string>(name: TName, details: TestDetails, fn: RowTestFn<TArgs, TCase, TName>): void;
};

type TestBody = (args: never) => Promise<void>;

export function bindTestEach<TTestArgs extends object, TWorkerArgs extends object>(
    testObj: TestType<TTestArgs, TWorkerArgs>,
    baseChannels?: string[],
    alsoForFirstRowChannels?: string[],
): TestEach<TTestArgs & TWorkerArgs> {
    return ((cases: readonly unknown[]) =>
        (name: string, detailsOrFn: TestDetails | TestBody, maybeFn?: TestBody) => {
            const [details, fn] =
                typeof detailsOrFn === 'function' ? [{}, detailsOrFn] : [detailsOrFn, maybeFn!];
            // The library only calls testObj.extend(rowFixtures) and then registers
            // the row's test on the result; this wrapper slots `details` into that call.
            const withDetails = {
                extend: (fixtures: Parameters<typeof testObj.extend>[0]) => {
                    const extended = testObj.extend(fixtures);
                    return (title: string, body: TestBody) => extended(title, details, body as never);
                },
            };
            bindUntypedTestEach(withDetails, baseChannels, alsoForFirstRowChannels)(cases)(name, fn as never);
        });
}
