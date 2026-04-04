import { OrderStatus } from '../entities/order-status.enum';

export class ViewOrderDetailsResponse {
  orderNumber: string;
  orderTimestamp: string;
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
  status: OrderStatus;
  country: string;
  appliedCouponCode: string | null;
}
