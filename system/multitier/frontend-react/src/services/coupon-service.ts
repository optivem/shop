import type { PublishCouponRequest, BrowseCouponsResponse } from '../types/api.types';
import { fetchJson, fetchNoContent } from '../common';
import { isBrowseCouponsResponse } from '../types/api.guards';
import type { Result } from '../types/result.types';

const API_BASE_URL = '/api/coupons';

// Publish-coupon returns 204 No Content — the backend (and the system tests)
// produce no body, so the result carries no data.
export async function createCoupon(
  code: string,
  discountRate: number,
  validFrom: string | null,
  validTo: string | null,
  usageLimit: number | null
): Promise<Result<void>> {
  const request: PublishCouponRequest = {
    code,
    discountRate,
    validFrom: validFrom ?? undefined,
    validTo: validTo ?? undefined,
    usageLimit: usageLimit ?? undefined
  };

  return await fetchNoContent(API_BASE_URL, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });
}

export async function browseCoupons(): Promise<Result<BrowseCouponsResponse>> {
  return await fetchJson(API_BASE_URL, isBrowseCouponsResponse, {
    method: 'GET'
  });
}
