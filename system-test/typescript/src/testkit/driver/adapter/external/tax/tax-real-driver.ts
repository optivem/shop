import { Result, success, failure } from '../../../../common/result.js';
import { ErrorResponse, GetTaxResponse, ReturnsTaxRateRequest } from '../../../../common/dtos.js';
import { TaxDriver } from '../../../port/external/tax/tax-driver.js';

export class TaxRealDriver implements TaxDriver {
  constructor(private baseUrl: string) {}

  async goToTax(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Tax service not available: ${response.status}`, fieldErrors: [] });
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/countries/${country}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; countryName?: string; taxRate: number };
      return success({ country: data.id || country, taxRate: data.taxRate });
    }
    return failure({ message: `Tax rate not found for country: ${country}`, fieldErrors: [] });
  }

  async returnsTaxRate(_request: ReturnsTaxRateRequest): Promise<Result<void, ErrorResponse>> {
    return success(undefined);
  }

  async close(): Promise<void> {}
}
