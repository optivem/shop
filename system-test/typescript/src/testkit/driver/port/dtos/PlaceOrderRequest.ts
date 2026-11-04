export interface PlaceOrderRequest {
  sku: string | null;
  quantity: string | null;
  country?: string | null;
  couponCode?: string | null;
  /** Sent as the `Idempotency-Key` header, never in the body; null/undefined means no header. */
  idempotencyKey?: string | null;
}
