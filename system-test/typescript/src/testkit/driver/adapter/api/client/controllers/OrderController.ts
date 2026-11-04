import type { Result } from '../../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../../port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../port/dtos/PlaceOrderResponse.js';
import type { BrowseOrderHistoryResponse } from '../../../../port/dtos/BrowseOrderHistoryResponse.js';
import type { ViewOrderResponse } from '../../../../port/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import type { BearerTokenSource } from '../../../shared/client/http/bearer-token-source.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';
import { queryString } from './query-string.js';

export class OrderController {
  private static readonly ENDPOINT = '/api/orders';
  private static readonly IDEMPOTENCY_KEY_HEADER = 'Idempotency-Key';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string, tokenSource?: BearerTokenSource) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse, tokenSource);
  }

  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    const { idempotencyKey, ...body } = request;
    const headers: Record<string, string> = idempotencyKey === null || idempotencyKey === undefined
      ? {}
      : { [OrderController.IDEMPOTENCY_KEY_HEADER]: idempotencyKey };
    return this.httpClient.post<PlaceOrderResponse>(OrderController.ENDPOINT, body, headers);
  }

  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.httpClient.get<ViewOrderResponse>(`${OrderController.ENDPOINT}/${orderNumber}`);
  }

  browseOrderHistory(
    orderNumber: string,
    page?: string | null,
    size?: string | null,
  ): Promise<Result<BrowseOrderHistoryResponse, SystemError>> {
    const query = queryString({
      orderNumber: orderNumber.trim() === '' ? null : orderNumber,
      page,
      size,
    });
    return this.httpClient.get<BrowseOrderHistoryResponse>(`${OrderController.ENDPOINT}${query}`);
  }

  cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(`${OrderController.ENDPOINT}/${orderNumber}/cancel`, {});
  }

  deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(`${OrderController.ENDPOINT}/${orderNumber}/deliver`, {});
  }
}
