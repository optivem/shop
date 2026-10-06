import type { Result } from '../../../common/result.js';
import { success, failure } from '../../../common/result.js';
import type { GoToMyShopRequest } from '../../port/dtos/GoToMyShopRequest.js';
import type { GoToMyShopResponse } from '../../port/dtos/GoToMyShopResponse.js';
import type { PlaceOrderRequest } from '../../port/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../port/dtos/PlaceOrderResponse.js';
import type { CancelOrderRequest } from '../../port/dtos/CancelOrderRequest.js';
import type { CancelOrderResponse } from '../../port/dtos/CancelOrderResponse.js';
import type { DeliverOrderRequest } from '../../port/dtos/DeliverOrderRequest.js';
import type { DeliverOrderResponse } from '../../port/dtos/DeliverOrderResponse.js';
import type { ViewOrderRequest } from '../../port/dtos/ViewOrderRequest.js';
import type { ViewOrderResponse } from '../../port/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../port/dtos/errors/SystemError.js';
import type { BrowseOrderHistoryRequest } from '../../port/dtos/BrowseOrderHistoryRequest.js';
import type { BrowseOrderHistoryResponse } from '../../port/dtos/BrowseOrderHistoryResponse.js';
import type { PublishCouponRequest } from '../../port/dtos/PublishCouponRequest.js';
import type { PublishCouponResponse } from '../../port/dtos/PublishCouponResponse.js';
import type { BrowseCouponsRequest } from '../../port/dtos/BrowseCouponsRequest.js';
import type { BrowseCouponsResponse } from '../../port/dtos/BrowseCouponsResponse.js';
import type { MyShopDriver } from '../../port/my-shop-driver.js';
import type { UserIdentity } from '../../port/user-identity.js';
import { testCustomer } from '../shared/client/http/test-user.js';
import { MyShopApiClient } from './client/MyShopApiClient.js';
import { ApiIdentity } from './client/api-identity.js';

export class MyShopApiDriver implements MyShopDriver {
  private readonly client: MyShopApiClient;

  constructor(baseUrl: string, keycloakBaseUrl?: string) {
    this.client = new MyShopApiClient(baseUrl, keycloakBaseUrl);
  }

  actAs(identity: UserIdentity): void {
    switch (identity.kind) {
      case 'DEFAULT':
        this.client.as(ApiIdentity.DEFAULT);
        break;
      case 'ANONYMOUS':
        this.client.as(ApiIdentity.ANONYMOUS);
        break;
      case 'CUSTOMER':
        this.client.asCustomer(testCustomer(identity.customerIndex));
        break;
      case 'ADMIN':
        this.client.as(ApiIdentity.ADMIN);
        break;
    }
  }

  async goToMyShop(_request: GoToMyShopRequest): Promise<Result<GoToMyShopResponse, SystemError>> {
    const result = await this.client.health().checkHealth();
    if (result.success) return success({});
    return failure(result.error);
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.client.orders().placeOrder(request);
  }

  async cancelOrder(request: CancelOrderRequest): Promise<Result<CancelOrderResponse, SystemError>> {
    const result = await this.client.orders().cancelOrder(request.orderNumber);
    if (result.success) return success({});
    return failure(result.error);
  }

  async deliverOrder(request: DeliverOrderRequest): Promise<Result<DeliverOrderResponse, SystemError>> {
    const result = await this.client.orders().deliverOrder(request.orderNumber);
    if (result.success) return success({});
    return failure(result.error);
  }

  async viewOrder(request: ViewOrderRequest): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.client.orders().viewOrder(request.orderNumber);
  }

  async browseOrderHistory(request: BrowseOrderHistoryRequest): Promise<Result<BrowseOrderHistoryResponse, SystemError>> {
    return this.client.orders().browseOrderHistory(request.orderNumber);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<PublishCouponResponse, SystemError>> {
    const result = await this.client.coupons().publishCoupon(request);
    if (result.success) return success({});
    return failure(result.error);
  }

  async browseCoupons(_request: BrowseCouponsRequest): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.client.coupons().browseCoupons();
  }

  async close(): Promise<void> {
    // No resources to release: the API client uses fetch per-call.
  }
}
