import type { Result } from './result.js';

export class ResultAssert<T, E> {
  constructor(private readonly result: Result<T, E>) {}

  isSuccess(): this {
    if (!this.result.success) {
      throw new Error(
        `Expected success but was failure: ${JSON.stringify(this.result.error)}`
      );
    }
    return this;
  }

  isFailure(): this {
    if (this.result.success) {
      throw new Error(
        `Expected failure but was success: ${JSON.stringify(this.result.value)}`
      );
    }
    return this;
  }

  getValue(): T {
    if (!this.result.success) {
      throw new Error(
        `Cannot get value from failure: ${JSON.stringify(this.result.error)}`
      );
    }
    return this.result.value;
  }

  getError(): E {
    if (this.result.success) {
      throw new Error(
        `Cannot get error from success: ${JSON.stringify(this.result.value)}`
      );
    }
    return this.result.error;
  }
}

export function assertThatResult<T, E>(result: Result<T, E>): ResultAssert<T, E> {
  return new ResultAssert(result);
}
