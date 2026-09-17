import type { Result } from '../../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../../port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../port/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../../port/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';
import { JsonHttpClient } from '../../../shared/client/http/json-http-client.js';
import { SystemErrorMapper } from '../../SystemErrorMapper.js';

export class OrderController {
  private static readonly ENDPOINT = '/api/orders';

  private readonly httpClient: JsonHttpClient<SystemError>;

  constructor(baseUrl: string) {
    this.httpClient = new JsonHttpClient(baseUrl, SystemErrorMapper.fromResponse);
  }

  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.httpClient.post<PlaceOrderResponse>(OrderController.ENDPOINT, request);
  }

  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.httpClient.get<ViewOrderResponse>(`${OrderController.ENDPOINT}/${orderNumber}`);
  }

  cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(`${OrderController.ENDPOINT}/${orderNumber}/cancel`, {});
  }

  deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.httpClient.postVoid(`${OrderController.ENDPOINT}/${orderNumber}/deliver`, {});
  }
}
