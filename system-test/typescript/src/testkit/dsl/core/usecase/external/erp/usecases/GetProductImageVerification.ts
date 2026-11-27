import { expect } from '@playwright/test';
import type { GetProductImageResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductImageResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

export class GetProductImageVerification extends ResponseVerification<GetProductImageResponse> {
  constructor(response: GetProductImageResponse, context: UseCaseContext) {
    super(response, context);
  }

  contentType(expectedContentType: string): this {
    expect(this.getResponse().contentType, 'Expected image content type').toBe(expectedContentType);
    return this;
  }

  notEmpty(): this {
    expect(this.getResponse().sizeInBytes, 'Expected the image to have content, but it was empty').toBeGreaterThan(0);
    return this;
  }
}
