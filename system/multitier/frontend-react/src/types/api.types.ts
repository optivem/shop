// API Request and Response types for Order operations

export enum OrderStatus {
  PLACED = 'PLACED',
  CANCELLED = 'CANCELLED',
  DELIVERED = 'DELIVERED',
}

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
  unitPrice: number;
  basePrice: number;
  discountRate: number;
  discountAmount: number;
  subtotalPrice: number;
  taxRate: number;
  taxAmount: number;
  totalPrice: number;
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
  totalPrice: number;
  appliedCouponCode: string | null;
  status: OrderStatus;
}

export interface BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryItemResponse[];
}

// Coupon API types

// The backend reports a coupon with no usage limit as Integer.MAX_VALUE.
export const UNLIMITED_USAGE_LIMIT = 2147483647;

export interface PublishCouponRequest {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
}

export interface BrowseCouponsItemResponse {
  code: string;
  discountRate: number;
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
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit: number | null;
  usedCount: number;
}
