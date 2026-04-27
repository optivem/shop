import type { Result } from '../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../port/myShop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../port/myShop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../port/myShop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../port/myShop/dtos/errors/SystemError.js';
import type { PublishCouponRequest } from '../../../port/myShop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../port/myShop/dtos/BrowseCouponsResponse.js';
import type { MyShopDriver } from '../../../port/myShop/my-shop-driver.js';
import { MyShopApiClient } from './client/MyShopApiClient.js';

export class MyShopApiDriver implements MyShopDriver {
  private readonly client: MyShopApiClient;

  constructor(baseUrl: string) {
    this.client = new MyShopApiClient(baseUrl);
  }

  async goToMyShop(): Promise<Result<void, SystemError>> {
    return this.client.health().checkHealth();
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.client.orders().placeOrder(request);
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.client.orders().cancelOrder(orderNumber);
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.client.orders().deliverOrder(orderNumber);
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.client.orders().viewOrder(orderNumber);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    return this.client.coupons().publishCoupon(request);
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.client.coupons().browseCoupons();
  }

  async close(): Promise<void> {
    // No resources to release: the API client uses fetch per-call.
  }
}
