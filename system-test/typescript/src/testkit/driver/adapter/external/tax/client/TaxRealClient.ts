import { Result, success, failure } from '../../../../../common/result.js';
import type { TaxErrorResponse } from '../../../../port/external/tax/dtos/TaxErrorResponse.js';
import type { GetTaxResponse } from '../../../../port/external/tax/dtos/GetTaxResponse.js';

export class TaxRealClient {
  constructor(private baseUrl: string) {}

  async checkHealth(): Promise<Result<void, TaxErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Tax service not available: ${response.status}` });
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/countries/${country}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; countryName?: string; taxRate: number };
      return success({ country: data.id || country, taxRate: data.taxRate });
    }
    return failure({ message: `Tax rate not found for country: ${country}` });
  }
}
