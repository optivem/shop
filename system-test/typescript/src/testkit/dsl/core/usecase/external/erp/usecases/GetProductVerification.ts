import { expect } from '@playwright/test';
import type { GetProductResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

export class GetProductVerification extends ResponseVerification<GetProductResponse> {
  constructor(response: GetProductResponse, context: UseCaseContext) {
    super(response, context);
  }

  sku(skuParamAlias: string): this {
    const expected = this.getContext().getParamValue(skuParamAlias);
    expect(this.getResponse().sku).toBe(expected);
    return this;
  }

  price(expectedPrice: number): this {
    expect(this.getResponse().price).toBe(expectedPrice);
    return this;
  }
}
