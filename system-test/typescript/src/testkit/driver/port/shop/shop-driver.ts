import { Result } from '../../../common/result.js';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  PublishCouponRequest,
  BrowseCouponsResponse,
} from '../../../common/dtos.js';

export interface ShopDriver {
  goToShop(): Promise<Result<void, ErrorResponse>>;
  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, ErrorResponse>>;
  cancelOrder(orderNumber: string): Promise<Result<void, ErrorResponse>>;
  deliverOrder(orderNumber: string): Promise<Result<void, ErrorResponse>>;
  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>>;
  publishCoupon(request: PublishCouponRequest): Promise<Result<void, ErrorResponse>>;
  browseCoupons(): Promise<Result<BrowseCouponsResponse, ErrorResponse>>;
  close(): Promise<void>;
}
