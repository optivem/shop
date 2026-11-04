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
}
