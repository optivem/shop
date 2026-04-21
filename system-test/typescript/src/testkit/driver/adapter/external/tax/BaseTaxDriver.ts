import type { Result } from '../../../../common/result.js';
import type { TaxErrorResponse } from '../../../port/external/tax/dtos/errors/TaxErrorResponse.js';
import type { GetCountryRequest } from '../../../port/external/tax/dtos/GetCountryRequest.js';
import type { GetTaxResponse } from '../../../port/external/tax/dtos/GetTaxResponse.js';
import type { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import type { TaxDriver } from '../../../port/external/tax/tax-driver.js';
import type { BaseTaxClient } from './client/BaseTaxClient.js';

export abstract class BaseTaxDriver<TClient extends BaseTaxClient> implements TaxDriver {
  protected constructor(protected readonly client: TClient) {}

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    return this.client.checkHealth();
  }

  async getTaxRate(request: GetCountryRequest): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    return this.client.getTaxRate(request.country);
  }

  abstract returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>>;
  abstract close(): Promise<void>;
}
