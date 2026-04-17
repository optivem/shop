import type { UseCaseContext } from './use-case-context.js';
import type { UseCaseResult } from './use-case-result.js';

/**
 * Shared base for use cases. Mirrors Java's `BaseUseCase<TDriver, TSuccessResponse, TSuccessVerification>`.
 * Subclasses accept a driver + the shared `UseCaseContext` and execute to a
 * `UseCaseResult` that exposes `.shouldSucceed()` / `.shouldFail()`.
 */
export abstract class BaseUseCase<TDriver, TSuccessResponse, TSuccessVerification> {
  protected constructor(
    protected readonly driver: TDriver,
    protected readonly context: UseCaseContext,
  ) {}

  abstract execute(): Promise<UseCaseResult<TSuccessResponse, TSuccessVerification>>;
}
