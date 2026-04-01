import { Result, success, failure } from '../common/result';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  ProblemDetailResponse,
} from '../common/dtos';
import { ShopDriver } from './types';

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

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/orders/${orderNumber}`);
    if (response.ok) {
      const data = (await response.json()) as ViewOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(mapProblemDetail(problemDetail));
  }

  async close(): Promise<void> {}
}
