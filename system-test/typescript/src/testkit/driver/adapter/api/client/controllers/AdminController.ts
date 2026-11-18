import type { Result } from '../../../../../common/result.js';
import type { BrowseCustomersResponse } from '../../../../port/dtos/BrowseCustomersResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import type { BearerTokenSource } from '../../../shared/client/http/bearer-token-source.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class AdminController {
  private static readonly ENDPOINT = '/api/admin';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string, tokenSource?: BearerTokenSource) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse, tokenSource);
  }

  browseCustomers(): Promise<Result<BrowseCustomersResponse, SystemError>> {
    return this.httpClient.get<BrowseCustomersResponse>(`${AdminController.ENDPOINT}/customers`);
  }
}
