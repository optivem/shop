export interface PlaceOrderRequest {
  sku: string;
  quantity: string | null;
  country?: string | null;
  couponCode?: string | null;
}
