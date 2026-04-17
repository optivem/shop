import type { Result } from '../../../../common/result.js';
import type { ShopDriver } from '../../../../driver/port/shop/shop-driver.js';
import type { PlaceOrderRequest } from '../../../../driver/port/shop/dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from '../../../../driver/port/shop/dtos/PlaceOrderResponse.js';
import type { ViewOrderResponse } from '../../../../driver/port/shop/dtos/ViewOrderResponse.js';
import type { SystemError } from '../../../../driver/port/shop/dtos/SystemError.js';
import type { PublishCouponRequest } from '../../../../driver/port/shop/dtos/PublishCouponRequest.js';
import type { BrowseCouponsResponse } from '../../../../driver/port/shop/dtos/BrowseCouponsResponse.js';

export class ShopDsl {
  constructor(private readonly driver: ShopDriver) {}

  async goToShop(): Promise<Result<void, SystemError>> {
    return this.driver.goToShop();
  }

  async placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>> {
    return this.driver.placeOrder(request);
  }

  async viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>> {
    return this.driver.viewOrder(orderNumber);
  }

  async cancelOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.driver.cancelOrder(orderNumber);
  }

  async deliverOrder(orderNumber: string): Promise<Result<void, SystemError>> {
    return this.driver.deliverOrder(orderNumber);
  }

  async publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>> {
    return this.driver.publishCoupon(request);
  }

  async browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>> {
    return this.driver.browseCoupons();
  }

  async close(): Promise<void> {
    await this.driver.close();
  }
}
