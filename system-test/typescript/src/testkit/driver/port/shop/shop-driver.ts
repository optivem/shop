import { Result } from '../../../common/result.js';
import { PlaceOrderRequest } from './dtos/PlaceOrderRequest.js';
import { PlaceOrderResponse } from './dtos/PlaceOrderResponse.js';
import { ViewOrderResponse } from './dtos/ViewOrderResponse.js';
import { SystemError } from './dtos/errors/SystemError.js';
import { PublishCouponRequest } from './dtos/PublishCouponRequest.js';
import { BrowseCouponsResponse } from './dtos/BrowseCouponsResponse.js';

export interface ShopDriver {
  goToShop(): Promise<Result<void, SystemError>>;
  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>>;
  cancelOrder(orderNumber: string): Promise<Result<void, SystemError>>;
  deliverOrder(orderNumber: string): Promise<Result<void, SystemError>>;
  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, SystemError>>;
  publishCoupon(request: PublishCouponRequest): Promise<Result<void, SystemError>>;
  browseCoupons(): Promise<Result<BrowseCouponsResponse, SystemError>>;
  close(): Promise<void>;
}
