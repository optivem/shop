import type { ShippingAddress } from './ShippingAddress.js';

export interface ViewOrderResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: string;
  basePrice?: string;
  discountRate?: string;
  discountAmount?: string;
  subtotalPrice?: string;
  taxRate?: string;
  taxAmount?: string;
  totalPrice: string;
  country?: string;
  appliedCouponCode?: string | null;
  status: string;
  /** Null for an order placed without an address (Quick order); not read by the UI channel. */
  shippingAddress?: ShippingAddress | null;
  /** UI only: whether the order details offer Cancel Order; unset on the API. */
  cancelOffered?: boolean | null;
  /** UI only: whether the order details offer Deliver Order; unset on the API. */
  deliverOffered?: boolean | null;
}
