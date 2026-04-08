import { Result, success, failure } from '../common/result.js';
import { ErrorResponse, GetTaxResponse, ReturnsTaxRateRequest } from '../common/dtos.js';
import { TaxDriver } from './types.js';
import { JsonWireMockClient } from '../clients/wiremock-client.js';

export class TaxStubDriver implements TaxDriver {
  private wireMock: JsonWireMockClient;

  constructor(private baseUrl: string) {
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async goToTax(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Tax stub not available: ${response.status}`, fieldErrors: [] });
  }

  async getTaxRate(country: string): Promise<Result<GetTaxResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/countries/${country}`);
    if (response.ok) {
      const data = (await response.json()) as { id?: string; countryName?: string; taxRate: number };
      return success({ country: data.id || country, taxRate: data.taxRate });
    }
    return failure({ message: `Tax rate not found for country: ${country}`, fieldErrors: [] });
  }

  async returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, ErrorResponse>> {
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
