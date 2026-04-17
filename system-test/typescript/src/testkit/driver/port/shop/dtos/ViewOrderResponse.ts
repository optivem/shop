export interface ViewOrderResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  basePrice?: number;
  discountRate?: number;
  discountAmount?: number;
  subtotalPrice?: number;
  taxRate?: number;
  taxAmount?: number;
  totalPrice: number;
  country?: string;
  appliedCouponCode?: string | null;
  status: string;
}
