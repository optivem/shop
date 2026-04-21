import { expect } from '@playwright/test';
import type { GetTaxResponse } from '../../../../../../driver/port/external/tax/dtos/GetTaxResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

export class GetTaxVerification extends ResponseVerification<GetTaxResponse> {
  constructor(response: GetTaxResponse, context: UseCaseContext) {
    super(response, context);
  }

  country(expectedCountryAlias: string): this {
    const expected = this.getContext().getParamValueOrLiteral(expectedCountryAlias);
    expect(this.getResponse().country).toBe(expected);
    return this;
  }

  taxRate(expectedTaxRate: number): this {
    expect(this.getResponse().taxRate).toBe(expectedTaxRate);
    return this;
  }

  taxRateIsPositive(): this {
    expect(this.getResponse().taxRate).toBeGreaterThan(0);
    return this;
  }
}
