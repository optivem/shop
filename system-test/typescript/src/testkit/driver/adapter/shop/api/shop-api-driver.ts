import type { Result } from '../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../port/shop/dtos/errors/SystemError.js';
import type { PublishCouponRequest } from '../../../port/shop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../port/shop/dtos/BrowseCouponsResponse.js';
import type { ShopDriver } from '../../../port/shop/shop-driver.js';
import { ShopApiClient } from './client/ShopApiClient.js';

export class ShopApiDriver implements ShopDriver {
  private readonly client: ShopApiClient;

  constructor(baseUrl: string) {
    this.client = new ShopApiClient(baseUrl);
  }

  async goToShop(): Promise<Result<void, SystemError>> {
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

  async close(): Promise<void> {}
}
