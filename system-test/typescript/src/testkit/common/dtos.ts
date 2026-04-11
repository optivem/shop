export interface PlaceOrderRequest {
  sku: string;
  quantity: string | null;
  country?: string | null;
  couponCode?: string | null;
}

export interface PlaceOrderResponse {
  orderNumber: string;
}

export interface ViewOrderResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  basePrice?: number;
  discountRate?: number;
  discountAmount?: number;
  subtotalPrice?: number;
  taxRate?: number;
  taxAmount?: number;
  totalPrice: number;
  country?: string;
  appliedCouponCode?: string | null;
  status: string;
}

export enum OrderStatus {
  PLACED = 'PLACED',
  CANCELLED = 'CANCELLED',
  DELIVERED = 'DELIVERED',
}

export interface ErrorResponse {
  message: string;
  fieldErrors: FieldError[];
}

export interface FieldError {
  field: string;
  message: string;
}

export interface ProblemDetailResponse {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  errors?: { field: string; message: string; code?: string; rejectedValue?: string }[];
}

export interface GetProductResponse {
  sku: string;
  price: number;
}

export interface ReturnsProductRequest {
  sku: string;
  price: string;
}

export interface ReturnsPromotionRequest {
  promotionActive: boolean;
  discount: string;
}

export interface PublishCouponRequest {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number | string;
}

export interface BrowseCouponItem {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount: number;
}

export interface BrowseCouponsResponse {
  coupons: BrowseCouponItem[];
}

export interface GetTimeResponse {
  time: string;
}

export interface ReturnsTimeRequest {
  time: string;
}

export interface GetTaxResponse {
  country: string;
  taxRate: number;
}

export interface ReturnsTaxRateRequest {
  country: string;
  taxRate: string;
}
