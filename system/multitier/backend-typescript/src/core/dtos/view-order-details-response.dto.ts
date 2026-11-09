import type Decimal from 'decimal.js';
import type { OrderStatus } from '../entities/order-status.enum';
import type { ShippingAddress } from './shipping-address.dto';

export class ViewOrderDetailsResponse {
  orderNumber!: string;
  orderTimestamp!: string;
  sku!: string;
  quantity!: number;
  unitPrice!: Decimal;
  basePrice!: Decimal;
  discountRate!: Decimal;
  discountAmount!: Decimal;
  subtotalPrice!: Decimal;
  taxRate!: Decimal;
  taxAmount!: Decimal;
  totalPrice!: Decimal;
  status!: OrderStatus;
  country!: string;
  appliedCouponCode!: string | null;
  shippingAddress!: ShippingAddress | null;
}
