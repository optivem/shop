import type { Result } from '../../../../../common/result.js';
import type { UpdateProfileRequest } from '../../../../port/dtos/UpdateProfileRequest.js';
import type { ViewProfileResponse } from '../../../../port/dtos/ViewProfileResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import type { BearerTokenSource } from '../../../shared/client/http/bearer-token-source.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class ProfileController {
  private static readonly ENDPOINT = '/api/account/profile';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string, tokenSource?: BearerTokenSource) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse, tokenSource);
  }

  viewProfile(): Promise<Result<ViewProfileResponse, SystemError>> {
    return this.httpClient.get<ViewProfileResponse>(ProfileController.ENDPOINT);
  }

  updateProfile(request: UpdateProfileRequest): Promise<Result<ViewProfileResponse, SystemError>> {
    return this.httpClient.put<ViewProfileResponse>(ProfileController.ENDPOINT, request);
  }
}
