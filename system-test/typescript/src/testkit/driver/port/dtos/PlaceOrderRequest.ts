export interface PlaceOrderRequest {
  sku: string | null;
  quantity: string | null;
  country?: string | null;
  couponCode?: string | null;
}
