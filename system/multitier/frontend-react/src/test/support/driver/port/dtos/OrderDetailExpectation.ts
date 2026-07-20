// Expected fields on the order-details screen. showsOrderDetails is a UI-only assertion (the gateway
// level does not render a screen), so these are the UI-formatted strings the screen shows — '$22.00',
// '10.00%' — mirroring the existing total-price style. Only the fields a spec names are asserted.
export interface OrderDetailExpectation {
  status?: string;
  sku?: string;
  country?: string;
  quantity?: string;
  unitPrice?: string;
  basePrice?: string;
  discountRate?: string;
  discountAmount?: string;
  subtotalPrice?: string;
  taxRate?: string;
  taxAmount?: string;
  totalPrice?: string;
  appliedCoupon?: string;
}
