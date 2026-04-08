import { Result, success, failure } from '../common/result.js';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  ProblemDetailResponse,
  PublishCouponRequest,
  ViewCouponResponse,
  BrowseCouponsResponse,
} from '../common/dtos.js';
import { ShopDriver } from './types.js';

function mapProblemDetail(pd: ProblemDetailResponse): ErrorResponse {
  return {
    message: pd.detail || 'Unknown error',
    fieldErrors: (pd.errors || []).map((e) => ({
      field: e.field,
      message: e.message,
    })),
  };
}

export class ShopApiDriver implements ShopDriver {
  constructor(private baseUrl: string) {}

  async goToShop(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Shop API not available: ${response.status}`, fieldErrors: [] });
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, ErrorResponse>> {
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

  async cancelOrder(orderNumber: string): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}/deliver`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}`);
    if (response.ok) {
      const data = (await response.json()) as ViewOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/coupons`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (response.ok) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async viewCoupon(code: string): Promise<Result<ViewCouponResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/coupons/${encodeURIComponent(code)}`);
    if (response.ok) {
      const data = (await response.json()) as ViewCouponResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/coupons`);
    if (response.ok) {
      const data = (await response.json()) as BrowseCouponsResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async close(): Promise<void> {}
}
