import type { Result } from '../../../../../common/result.js';
import type { TaxDriver } from '../../../../../driver/port/external/tax/tax-driver.js';
import type { TaxErrorResponse } from '../../../../../driver/port/external/tax/dtos/TaxErrorResponse.js';
import type { GetTaxResponse } from '../../../../../driver/port/external/tax/dtos/GetTaxResponse.js';
import type { ReturnsTaxRateRequest } from '../../../../../driver/port/external/tax/dtos/ReturnsTaxRateRequest.js';

export class TaxDsl {
  constructor(private readonly driver: TaxDriver) {}

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    return this.driver.goToTax();
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    return this.driver.getTaxRate({ country });
  }

  async returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    return this.driver.returnsTaxRate(request);
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
