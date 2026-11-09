export interface BrowseOrderHistoryRequest {
  orderNumber: string;
  page?: string | null;
  size?: string | null;
  /** Username of the customer whose orders an admin wants to see (sent as `customer`). */
  customer?: string | null;
}
