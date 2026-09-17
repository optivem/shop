// Typed facade: the library declares the test callback's argument as `any`; here fixtures and row shape flow through.
import { bindTestEach as bindUntypedTestEach } from '@optivem/optivem-testing';
import type { TestType } from '@playwright/test';

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

export type TestEach<TArgs> = <TCase>(
    cases: readonly TCase[],
) => <TName extends string>(name: TName, fn: (args: TArgs & RowFixtures<TCase, TName>) => Promise<void>) => void;

export function bindTestEach<TTestArgs extends object, TWorkerArgs extends object>(
    testObj: TestType<TTestArgs, TWorkerArgs>,
    baseChannels?: string[],
    alsoForFirstRowChannels?: string[],
): TestEach<TTestArgs & TWorkerArgs> {
    return bindUntypedTestEach(testObj, baseChannels, alsoForFirstRowChannels);
}
