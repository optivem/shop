// API Request and Response types for Order operations

export const OrderStatus = {
  PLACED: 'PLACED',
  CANCELLED: 'CANCELLED',
  DELIVERED: 'DELIVERED',
} as const;

export type OrderStatus = (typeof OrderStatus)[keyof typeof OrderStatus];

// The address an order ships to; its country is the order's tax country.
export interface ShippingAddress {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
}

// API Request types
// Quick order sends a country; checkout sends a full shipping address instead.
export interface PlaceOrderRequest {
  sku: string;
  quantity: number;
  country?: string;
  shippingAddress?: ShippingAddress;
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
  shippingAddress?: ShippingAddress | null; // absent for Quick orders and older orders
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
  totalPages?: number; // paging metadata; absent from older backends
}

export interface BrowseOrderHistoryQuery {
  orderNumber?: string;
  customer?: string;
  page?: number;
}

// Catalog types (GET /api/products, GET /api/products/{sku})
export interface Product {
  sku: string;
  title: string;
  description: string;
  brand: string;
  category: string;
  price: string;
}

// Customer profile types (GET/PUT /api/account/profile)
export interface ProfileResponse {
  firstName: string | null;
  lastName: string | null;
  email: string | null;
  street: string | null;
  postalCode: string | null;
  city: string | null;
  country: string | null;
}

export interface UpdateProfileRequest {
  firstName: string;
  lastName: string;
  street: string;
  postalCode: string;
  city: string;
  country: string;
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
