export interface BrowseOrderHistoryOrderItem {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  country: string;
  quantity: number;
  totalPrice: string;
  status: string;
  appliedCouponCode?: string | null;
  customer?: string;
  /** UI only: where the order's "View Details" link goes. */
  detailsPath?: string | null;
}

export interface BrowseOrderHistoryResponse {
  orders: BrowseOrderHistoryOrderItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  /** UI only: the customer filter's choices as shown ("All customers", then usernames); unset when there is no filter. */
  customerFilterOptions?: string[] | null;
  /** UI only: the customer filter's chosen option as shown, such as "All customers"; unset when there is no filter. */
  customerFilterSelection?: string | null;
  /** UI only: whether the list has a Customer column. */
  customerColumnShown?: boolean | null;
  /** UI only: the message the list shows instead of orders, such as "No orders found". */
  message?: string | null;
}
