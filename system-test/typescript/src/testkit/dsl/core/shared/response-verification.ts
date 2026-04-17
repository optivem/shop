import type { UseCaseContext } from './use-case-context.js';

export class ResponseVerification<TResponse> {
  constructor(
    private readonly response: TResponse,
    private readonly context: UseCaseContext,
  ) {}

  protected getResponse(): TResponse {
    return this.response;
  }

  protected getContext(): UseCaseContext {
    return this.context;
  }
}
