import { Result, success, failure } from '../../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../../port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../../port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../../port/shop/dtos/SystemError.js';
import type { ProblemDetailResponse } from '../../../../port/shop/dtos/ProblemDetailResponse.js';
import type { PublishCouponRequest } from '../../../../port/shop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../../port/shop/dtos/BrowseCouponsResponse.js';

function mapProblemDetail(pd: ProblemDetailResponse): SystemError {
  return {
    message: pd.detail || 'Unknown error',
    fieldErrors: (pd.errors || []).map((e) => ({
      field: e.field,
      message: e.message,
    })),
  };
}

export class ShopApiClient {
  constructor(private baseUrl: string) {}

  async health(): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Shop API not available: ${response.status}`, fieldErrors: [] });
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (response.ok) {
      const data = (await response.json()) as PlaceOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}/deliver`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}`);
    if (response.ok) {
      const data = (await response.json()) as ViewOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/coupons`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (response.ok) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}/api/coupons`);
    if (response.ok) {
      const data = (await response.json()) as BrowseCouponsResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }
}
