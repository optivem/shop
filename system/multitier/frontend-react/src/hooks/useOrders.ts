import { useState, useEffect, useCallback } from 'react';
import { adminService } from '../services/admin-service';
import { orderService } from '../services/order-service';
import type { BrowseOrderHistoryItemResponse } from '../types/api.types';

interface OrdersRequest {
  filter: string;
  customer: string;
  reloadCount: number;
}

interface UseOrdersOptions {
  /** Admins list every customer's orders and get the customers who have ordered, for the customer filter. */
  loadCustomers?: boolean;
}

const withCustomer = (known: string[], chosen: string) => [...known, chosen].sort((a, b) => a.localeCompare(b));

/**
 * Custom hook for managing order history browsing and filtering
 * @param initialFilter - Optional initial filter value for order number
 * @returns Order history state and control functions
 */
export function useOrders(initialFilter = '', { loadCustomers = false }: UseOrdersOptions = {}) {
  const [orders, setOrders] = useState<BrowseOrderHistoryItemResponse[]>([]);
  const [filter, setFilter] = useState(initialFilter);
  const [customer, setCustomer] = useState('');
  const [customers, setCustomers] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);
  // The request whose response is currently applied; loading is derived from it.
  const [loaded, setLoaded] = useState<OrdersRequest | null>(null);

  useEffect(() => {
    // The filter is typed keystroke by keystroke, so responses can arrive out of order:
    // ignore any response superseded by a newer filter, refresh, or unmount.
    let ignore = false;
    void orderService.browseOrderHistory({ orderNumber: filter, customer }).then((result) => {
      if (ignore) return;
      if (result.success) {
        setOrders(result.data.orders);
        setError(null);
      } else {
        setError(result.error.message);
      }
      setLoaded({ filter, customer, reloadCount });
    });
    return () => {
      ignore = true;
    };
  }, [filter, customer, reloadCount]);

  // The customers for the filter come from their own admin endpoint, not from the order list shown (which the
  // admin may already have filtered before it arrives). So a newer filter never cancels this request; only a
  // refresh (which fetches the list again) or unmounting does.
  useEffect(() => {
    if (!loadCustomers) return;
    let ignore = false;
    void adminService.browseCustomers().then((result) => {
      if (!ignore && result.success) setCustomers(result.data);
    });
    return () => {
      ignore = true;
    };
  }, [loadCustomers, reloadCount]);

  const refresh = useCallback(() => {
    setReloadCount((count) => count + 1);
  }, []);

  const isLoading = loaded?.filter !== filter || loaded.customer !== customer || loaded.reloadCount !== reloadCount;

  return {
    orders,
    filter,
    setFilter,
    customer,
    setCustomer,
    // The chosen customer stays offered even before the list of customers has arrived.
    customers: customer && !customers.includes(customer) ? withCustomer(customers, customer) : customers,
    isLoading,
    error,
    refresh
  };
}
