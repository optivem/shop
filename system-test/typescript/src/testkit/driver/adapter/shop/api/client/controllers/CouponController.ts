import type { Result } from '../../../../../../common/result.js';
import { success, failure } from '../../../../../../common/result.js';
import type { PublishCouponRequest } from '../../../../../port/shop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../../../port/shop/dtos/BrowseCouponsResponse.js';
import type { SystemError } from '../../../../../port/shop/dtos/SystemError.js';
import type { ProblemDetailResponse } from '../dtos/errors/ProblemDetailResponse.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class CouponController {
  private static readonly ENDPOINT = '/api/coupons';

  constructor(private readonly baseUrl: string) {}

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}${CouponController.ENDPOINT}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (response.ok) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}${CouponController.ENDPOINT}`);
    if (response.ok) {
      const data = (await response.json()) as BrowseCouponsResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }
}
