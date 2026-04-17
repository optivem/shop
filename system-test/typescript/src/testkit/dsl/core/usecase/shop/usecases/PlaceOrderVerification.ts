import { expect } from '@playwright/test';
import type { PlaceOrderResponse } from '../../../../../driver/port/shop/dtos/PlaceOrderResponse.js';
import { ResponseVerification } from '../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';

export class PlaceOrderVerification extends ResponseVerification<PlaceOrderResponse> {
  constructor(response: PlaceOrderResponse, context: UseCaseContext) {
    super(response, context);
  }

  orderNumber(aliasOrValue: string): this {
    const expected = this.getContext().getResultValue(aliasOrValue);
    expect(this.getResponse().orderNumber).toBe(expected);
    return this;
  }

  orderNumberStartsWith(prefix: string): this {
    expect(this.getResponse().orderNumber.startsWith(prefix)).toBe(true);
    return this;
  }
}
