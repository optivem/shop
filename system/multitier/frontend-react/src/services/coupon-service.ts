import { PublishCouponRequest, CreateCouponResponse, BrowseCouponsResponse } from '../types/api.types';
import { fetchJson } from '../common';
import type { Result } from '../types/result.types';

const API_BASE_URL = '/api/coupons';

export async function createCoupon(
  code: string,
  discountRate: number,
  validFrom: string | null,
  validTo: string | null,
  usageLimit: number | null
): Promise<Result<CreateCouponResponse>> {
  const request: PublishCouponRequest = {
    code,
    discountRate,
    validFrom: validFrom ?? undefined,
    validTo: validTo ?? undefined,
    usageLimit: usageLimit ?? undefined
  };

  return await fetchJson<CreateCouponResponse>(API_BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
}

export async function browseCoupons(): Promise<Result<BrowseCouponsResponse>> {
  return await fetchJson<BrowseCouponsResponse>(API_BASE_URL, {
    method: 'GET'
  });
}
