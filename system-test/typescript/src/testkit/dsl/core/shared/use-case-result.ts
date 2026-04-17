import type { Result } from '../../../common/result.js';
import type { SystemError } from '../../../driver/port/shop/dtos/SystemError.js';
import { ErrorVerification } from './error-verification.js';
import type { UseCaseContext } from './use-case-context.js';

export type SuccessVerificationFactory<TResponse, TVerification> = (
  response: TResponse,
  context: UseCaseContext,
) => TVerification;

export class UseCaseResult<TResponse, TSuccessVerification> {
  constructor(
    private readonly result: Result<TResponse, SystemError>,
    private readonly context: UseCaseContext,
    private readonly successVerificationFactory: SuccessVerificationFactory<TResponse, TSuccessVerification>,
  ) {}

  shouldSucceed(): TSuccessVerification {
    if (!this.result.success) {
      throw new Error(`Expected success but was failure: ${JSON.stringify(this.result.error)}`);
    }
    return this.successVerificationFactory(this.result.value, this.context);
  }

  shouldFail(): ErrorVerification {
    if (this.result.success) {
      throw new Error(`Expected failure but was success: ${JSON.stringify(this.result.value)}`);
    }
    return new ErrorVerification(this.result.error, this.context);
  }
}
