import { ResponseVerification } from './response-verification.js';
import type { UseCaseContext } from './use-case-context.js';

export class VoidVerification extends ResponseVerification<void> {
  constructor(response: void, context: UseCaseContext) {
    super(response, context);
  }
}
