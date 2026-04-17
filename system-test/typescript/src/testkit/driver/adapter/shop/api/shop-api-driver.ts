import type { Result } from '../../../../common/result.js';
import type { PlaceOrderRequest } from '../../../port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../port/shop/dtos/SystemError.js';
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
    return this.client.health();
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.client.placeOrder(request);
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.client.cancelOrder(orderNumber);
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.client.deliverOrder(orderNumber);
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.client.viewOrder(orderNumber);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    return this.client.publishCoupon(request);
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.client.browseCoupons();
  }

  async close(): Promise<void> {}
}
