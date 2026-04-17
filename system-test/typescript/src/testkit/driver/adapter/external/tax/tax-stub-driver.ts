import type { Result } from '../../../../common/result.js';
import type { TaxErrorResponse } from '../../../port/external/tax/dtos/TaxErrorResponse.js';
import type { GetTaxResponse } from '../../../port/external/tax/dtos/GetTaxResponse.js';
import type { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import type { TaxDriver } from '../../../port/external/tax/tax-driver.js';
import { TaxStubClient } from './client/TaxStubClient.js';

export class TaxStubDriver implements TaxDriver {
  private readonly client: TaxStubClient;

  constructor(baseUrl: string) {
    this.client = new TaxStubClient(baseUrl);
  }

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    return this.client.checkHealth();
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    return this.client.getTaxRate(country);
  }

  async returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    return this.client.configureTaxRate(request);
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
