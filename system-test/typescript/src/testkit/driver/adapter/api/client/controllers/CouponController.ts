import type { Result } from '../../../../../common/result.js';
import type { PublishCouponRequest } from '../../../../port/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../../port/dtos/BrowseCouponsResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import type { BearerTokenSource } from '../../../shared/client/http/bearer-token-source.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';
import { queryString } from './query-string.js';

export class CouponController {
  private static readonly ENDPOINT = '/api/coupons';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string, tokenSource?: BearerTokenSource) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse, tokenSource);
  }

  publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(CouponController.ENDPOINT, request);
  }

  browseCoupons(page?: string | null, size?: string | null): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.httpClient.get<BrowseCouponsResponse>(`${CouponController.ENDPOINT}${queryString({ page, size })}`);
  }
}
