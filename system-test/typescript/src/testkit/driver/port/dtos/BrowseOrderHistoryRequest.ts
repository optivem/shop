/** `followUp`: after browsing, set the customer filter to All customers and clear the order number (UI only). */
export const RESET_FILTERS = 'reset-filters';

export interface BrowseOrderHistoryRequest {
  orderNumber: string;
  page?: string | null;
  size?: string | null;
  /** Username of the customer whose orders an admin wants to see (sent as `customer`). */
  customer?: string | null;
  /** What the user does after browsing: RESET_FILTERS, or nothing (unset). UI only. */
  followUp?: string | null;
}
