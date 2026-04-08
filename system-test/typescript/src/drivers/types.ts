import { Result } from '../common/result.js';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  GetProductResponse,
  ReturnsProductRequest,
  ReturnsPromotionRequest,
  GetTimeResponse,
  ReturnsTimeRequest,
  GetTaxResponse,
  ReturnsTaxRateRequest,
  PublishCouponRequest,
  ViewCouponResponse,
  BrowseCouponsResponse,
} from '../common/dtos.js';

export interface ShopDriver {
  goToShop(): Promise<Result<void, ErrorResponse>>;
  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, ErrorResponse>>;
  cancelOrder(orderNumber: string): Promise<Result<void, ErrorResponse>>;
  deliverOrder(orderNumber: string): Promise<Result<void, ErrorResponse>>;
  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>>;
  publishCoupon(request: PublishCouponRequest): Promise<Result<void, ErrorResponse>>;
  viewCoupon(code: string): Promise<Result<ViewCouponResponse, ErrorResponse>>;
  browseCoupons(): Promise<Result<BrowseCouponsResponse, ErrorResponse>>;
  close(): Promise<void>;
}

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErrorResponse>>;
  getProduct(sku: string): Promise<Result<GetProductResponse, ErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErrorResponse>>;
  returnsPromotion(request: ReturnsPromotionRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}

export interface ClockDriver {
  goToClock(): Promise<Result<void, ErrorResponse>>;
  getTime(): Promise<Result<GetTimeResponse, ErrorResponse>>;
  returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}

export interface TaxDriver {
  goToTax(): Promise<Result<void, ErrorResponse>>;
  getTaxRate(country: string): Promise<Result<GetTaxResponse, ErrorResponse>>;
  returnsTaxRate(request: ReturnsTaxRateRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}
