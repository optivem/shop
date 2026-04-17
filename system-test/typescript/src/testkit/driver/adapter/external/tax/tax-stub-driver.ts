import { Result, success, failure } from '../../../../common/result.js';
import { TaxErrorResponse } from '../../../port/external/tax/dtos/TaxErrorResponse.js';
import { GetTaxResponse } from '../../../port/external/tax/dtos/GetTaxResponse.js';
import { ReturnsTaxRateRequest } from '../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import { TaxDriver } from '../../../port/external/tax/tax-driver.js';
import { JsonWireMockClient } from '../../shared/wiremock/wiremock-client.js';

export class TaxStubDriver implements TaxDriver {
  private wireMock: JsonWireMockClient;

  constructor(private baseUrl: string) {
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async goToTax(): Promise<Result<void, TaxErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Tax stub not available: ${response.status}` });
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, TaxErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/countries/${country}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; countryName?: string; taxRate: number };
      return success({ country: data.id || country, taxRate: data.taxRate });
    }
    return failure({ message: `Tax rate not found for country: ${country}` });
  }

  async returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    await this.wireMock.stubGet(`/tax/api/countries/${request.country}`, {
      id: request.country,
      countryName: request.country,
      taxRate: parseFloat(request.taxRate),
    });
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
