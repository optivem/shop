export interface BrowseOrderHistoryOrderItem {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  country: string;
  quantity: number;
  totalPrice: number;
  status: string;
  appliedCouponCode?: string | null;
  customer?: string;
}

export interface BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryOrderItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
