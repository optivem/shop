import { OrderStatus } from '../entities/order-status.enum';

export class ViewOrderDetailsResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: OrderStatus;
}
