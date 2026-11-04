// API Request and Response types for Order operations

export const OrderStatus = {
  PLACED: 'PLACED',
  CANCELLED: 'CANCELLED',
  DELIVERED: 'DELIVERED',
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

// API Request types
export interface PlaceOrderRequest {
  sku: string;
  quantity: number;
  country: string;
  couponCode?: string;
}

// API Response types
export interface PlaceOrderResponse {
  orderNumber: string;
}

export interface ViewOrderDetailsResponse {
  orderNumber: string;
  orderTimestamp: string; // ISO 8601 date string
  country: string;
  sku: string;
  quantity: number;
  unitPrice: string;
  basePrice: string;
  discountRate: string;
  discountAmount: string;
  subtotalPrice: string;
  taxRate: string;
  taxAmount: string;
  totalPrice: string;
  appliedCouponCode: string | null;
  status: OrderStatus;
}

// Browse Order History types
export interface BrowseOrderHistoryItemResponse {
  orderNumber: string;
  orderTimestamp: string; // ISO 8601 date string
  country: string;
  sku: string;
  quantity: number;
  totalPrice: string;
  appliedCouponCode: string | null;
  status: OrderStatus;
  customer?: string | null; // owner of the order; only some backends report it
}

export interface BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryItemResponse[];
}

// Coupon API types

// The backend reports a coupon with no usage limit as Integer.MAX_VALUE.
export const UNLIMITED_USAGE_LIMIT = 2147483647;

export interface PublishCouponRequest {
  code: string;
  discountRate: string | null;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
}

export interface BrowseCouponsItemResponse {
  code: string;
  discountRate: string;
  validFrom?: string | null;
  validTo?: string | null;
  usageLimit: number | null;
  usedCount: number;
}

export interface BrowseCouponsResponse {
  coupons: BrowseCouponsItemResponse[];
}

export interface GetCouponResponse {
  code: string;
  discountRate: string;
  validFrom?: string;
  validTo?: string;
  usageLimit: number | null;
  usedCount: number;
}
