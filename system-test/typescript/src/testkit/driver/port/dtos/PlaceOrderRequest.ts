import type { ShippingAddress } from './ShippingAddress.js';

export interface PlaceOrderRequest {
  sku: string | null;
  quantity: string | null;
  country?: string | null;
  couponCode?: string | null;
  /** When set, the tax country is the address's and the top-level country is not sent. */
  shippingAddress?: ShippingAddress | null;
  /** Sent as the `Idempotency-Key` header, never in the body; null/undefined means no header. */
  idempotencyKey?: string | null;
}
