/**
 * Given/When stages expose a zero-arg `then()` for the fluent chain, which makes
 * them thenables: `await scenario.when().placeOrder()` makes JS call
 * `then(resolve, reject)`, which never settles, so the test hangs until timeout.
 *
 * DSL callers always invoke `then()` with no arguments; Promise resolution always
 * passes the resolve/reject callbacks. Throwing synchronously here rejects the
 * pending `await`, so an incomplete scenario fails fast with a clear message.
 */
export function assertNotAwaited(args: readonly unknown[]): void {
  if (args.length > 0) {
    throw new Error('Incomplete scenario: add .then().shouldSucceed()/shouldFail()');
  }
}
