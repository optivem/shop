import type { Result } from '../../../../common/result.js';
import type { TaxErrorResponse } from '../../../port/external/tax/dtos/errors/TaxErrorResponse.js';
import type { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import { BaseTaxDriver } from './BaseTaxDriver.js';
import { TaxStubClient } from './client/TaxStubClient.js';

export class TaxStubDriver extends BaseTaxDriver<TaxStubClient> {
  constructor(baseUrl: string) {
    super(new TaxStubClient(baseUrl));
  }

  async returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    return this.client.configureTaxRate(request);
  }

  async close(): Promise<void> {
    await this.client.close();
  }
}
