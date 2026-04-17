import type { Result } from '../../../../common/result.js';
import { success } from '../../../../common/result.js';
import type { TaxErrorResponse } from '../../../port/external/tax/dtos/TaxErrorResponse.js';
import type { GetTaxResponse } from '../../../port/external/tax/dtos/GetTaxResponse.js';
import type { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import type { TaxDriver } from '../../../port/external/tax/tax-driver.js';
import { TaxRealClient } from './client/TaxRealClient.js';

export class TaxRealDriver implements TaxDriver {
  private readonly client: TaxRealClient;

  constructor(baseUrl: string) {
    this.client = new TaxRealClient(baseUrl);
  }

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    return this.client.checkHealth();
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    return this.client.getTaxRate(country);
  }

  async returnsTaxRate(_request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
