import { Result } from '../common/result';
import {
  PlaceOrderRequest,
  PlaceOrderResponse,
  ViewOrderResponse,
  ErrorResponse,
  GetProductResponse,
  ReturnsProductRequest,
  GetTimeResponse,
  ReturnsTimeRequest,
} from '../common/dtos';

export interface ShopDriver {
  goToShop(): Promise<Result<void, ErrorResponse>>;
  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, ErrorResponse>>;
  viewOrder(orderNumber: string): Promise<Result<ViewOrderResponse, ErrorResponse>>;
  close(): Promise<void>;
}

export interface ErpDriver {
  goToErp(): Promise<Result<void, ErrorResponse>>;
  getProduct(sku: string): Promise<Result<GetProductResponse, ErrorResponse>>;
  returnsProduct(request: ReturnsProductRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}

export interface ClockDriver {
  goToClock(): Promise<Result<void, ErrorResponse>>;
  getTime(): Promise<Result<GetTimeResponse, ErrorResponse>>;
  returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ErrorResponse>>;
  close(): Promise<void>;
}
