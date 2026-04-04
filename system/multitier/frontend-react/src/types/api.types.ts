// API Request and Response types for Order operations

export enum OrderStatus {
  PLACED = 'PLACED',
}

// API Request types
export interface PlaceOrderRequest {
  sku: string;
  quantity: number;
  country?: string;
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
