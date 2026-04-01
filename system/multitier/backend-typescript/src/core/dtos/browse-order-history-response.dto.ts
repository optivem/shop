import { OrderStatus } from '../entities/order-status.enum';

export class BrowseOrderHistoryItemResponse {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  totalPrice: number;
  status: OrderStatus;
}

export class BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryItemResponse[];
}
