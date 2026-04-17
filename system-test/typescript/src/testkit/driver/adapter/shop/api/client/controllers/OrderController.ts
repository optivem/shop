import type { Result } from '../../../../../../common/result.js';
import { success, failure } from '../../../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../../../port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../../port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../../../port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../../../port/shop/dtos/SystemError.js';
import type { ProblemDetailResponse } from '../dtos/errors/ProblemDetailResponse.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class OrderController {
  private static readonly ENDPOINT = '/api/orders';

  constructor(private readonly baseUrl: string) {}

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}${OrderController.ENDPOINT}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });

    if (response.ok) {
      const data = (await response.json()) as PlaceOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    const response = await fetch(`${this.baseUrl}${OrderController.ENDPOINT}/${orderNumber}`);
    if (response.ok) {
      const data = (await response.json()) as ViewOrderResponse;
      return success(data);
    }

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}${OrderController.ENDPOINT}/${orderNumber}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}${OrderController.ENDPOINT}/${orderNumber}/deliver`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({}),
    });

    if (response.ok || response.status === 204) return success(undefined);

    const problemDetail = (await response.json()) as ProblemDetailResponse;
    return failure(SystemErrorMapper.from(problemDetail));
  }
}
