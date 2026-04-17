import { expect } from '@playwright/test';
import type { SystemError } from '../../../driver/port/shop/dtos/SystemError.js';
import { ResponseVerification } from './response-verification.js';
import type { UseCaseContext } from './use-case-context.js';

export class ErrorVerification extends ResponseVerification<SystemError> {
  constructor(error: SystemError, context: UseCaseContext) {
    super(error, context);
  }

  errorMessage(expectedMessage: string): this {
    const expanded = this.getContext().expandAliases(expectedMessage);
    expect(this.getResponse().message).toBe(expanded);
    return this;
  }

  fieldErrorMessage(expectedField: string, expectedMessage: string): this {
    const expandedField = this.getContext().expandAliases(expectedField);
    const expandedMessage = this.getContext().expandAliases(expectedMessage);
    const fields = this.getResponse().fieldErrors ?? [];
    const matching = fields.find((f) => f.field === expandedField);
    expect(matching, `Expected field error for '${expandedField}' in ${JSON.stringify(fields)}`).toBeDefined();
    expect(matching!.message).toBe(expandedMessage);
    return this;
  }
}
