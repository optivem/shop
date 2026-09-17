import type { Result } from '../../../../../common/result.js';
import type { PublishCouponRequest } from '../../../../port/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../../port/dtos/BrowseCouponsResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class CouponController {
  private static readonly ENDPOINT = '/api/coupons';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse);
  }

  publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(CouponController.ENDPOINT, request);
  }

  browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.httpClient.get<BrowseCouponsResponse>(CouponController.ENDPOINT);
  }
}
