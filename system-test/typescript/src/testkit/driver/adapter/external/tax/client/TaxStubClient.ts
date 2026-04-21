import { Result, success } from '../../../../../common/result.js';
import type { TaxErrorResponse } from '../../../../port/external/tax/dtos/errors/TaxErrorResponse.js';
import type { ReturnsTaxRateRequest } from '../../../../port/external/tax/dtos/ReturnsTaxRateRequest.js';
import { JsonWireMockClient } from '../../../shared/client/wiremock/json-wiremock-client.js';
import { BaseTaxClient } from './BaseTaxClient.js';
import type { ExtGetCountryResponse } from './dtos/ExtGetCountryResponse.js';

export class TaxStubClient extends BaseTaxClient {
  private readonly wireMock: JsonWireMockClient;

  constructor(baseUrl: string) {
    super(baseUrl);
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async configureTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, TaxErrorResponse>> {
    const stubBody: ExtGetCountryResponse = {
      id: request.country,
      countryName: request.country,
      taxRate: Number.parseFloat(request.taxRate),
    };
    await this.wireMock.stubGet(`/tax/api/countries/${request.country}`, stubBody);
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
