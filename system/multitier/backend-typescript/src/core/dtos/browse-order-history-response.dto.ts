import { OrderStatus } from '../entities/order-status.enum';

export class BrowseOrderHistoryItemResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  country: string;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
  appliedCouponCode: string | null;
}

export class BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryItemResponse[];
}
