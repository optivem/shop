import { useState, useEffect, useCallback, useRef } from 'react';
import { orderService } from '../services/order-service';
import type { BrowseOrderHistoryItemResponse, BrowseOrderHistoryResponse } from '../types/api.types';
import type { Result } from '../types/result.types';

interface OrdersRequest {
  filter: string;
  customer: string;
  reloadCount: number;
}

interface UseOrdersOptions {
  /** Admins list every customer's orders and get the customers who have ordered, for the customer filter. */
  collectCustomers?: boolean;
}

// The admin list is paged; without paging metadata the first page is all there is.
const MAX_CUSTOMER_PAGES = 50;

function customersIn(orders: BrowseOrderHistoryItemResponse[]): string[] {
  return orders.map((order) => order.customer).filter((customer): customer is string => !!customer);
}

/**
 * The customers who have placed orders, derived from the unfiltered order list the admin already loads:
 * its first page, plus the remaining pages when the backend reports more than one.
 */
async function collectAllCustomers(
  first: BrowseOrderHistoryResponse,
  fetchPage: (page: number) => Promise<Result<BrowseOrderHistoryResponse>>,
): Promise<string[]> {
  const customers = customersIn(first.orders);
  const totalPages = Math.min(first.totalPages ?? 1, MAX_CUSTOMER_PAGES);
  for (let page = 2; page <= totalPages; page++) {
    const result = await fetchPage(page);
    if (!result.success) break;
    customers.push(...customersIn(result.data.orders));
  }
  return customers;
}

const mergeCustomers = (known: string[], more: string[]) => [...new Set([...known, ...more])].sort((a, b) => a.localeCompare(b));

/**
 * Custom hook for managing order history browsing and filtering
 * @param initialFilter - Optional initial filter value for order number
 * @returns Order history state and control functions
 */
export function useOrders(initialFilter = '', { collectCustomers = false }: UseOrdersOptions = {}) {
  const [orders, setOrders] = useState<BrowseOrderHistoryItemResponse[]>([]);
  const [filter, setFilter] = useState(initialFilter);
  const [customer, setCustomer] = useState('');
  const [customers, setCustomers] = useState<string[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);
  // The request whose response is currently applied; loading is derived from it.
  const [loaded, setLoaded] = useState<OrdersRequest | null>(null);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
    };
  }, []);

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

  // The customers for the filter come from the unfiltered list, fetched on its own: the list shown may already be
  // filtered (the admin can type before the first page arrives). Collecting takes a request per page, and it only
  // ever adds customers, so a newer filter or refresh does not cancel it; only unmounting does.
  useEffect(() => {
    if (!collectCustomers) return;
    const fetchPage = (page: number) => orderService.browseOrderHistory({ page });
    void fetchPage(1).then(async (first) => {
      if (!first.success || !mounted.current) return;
      const found = await collectAllCustomers(first.data, fetchPage);
      if (mounted.current) setCustomers((known) => mergeCustomers(known, found));
    });
  }, [collectCustomers, reloadCount]);

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
    customers: customer && !customers.includes(customer) ? mergeCustomers(customers, [customer]) : customers,
    isLoading,
    error,
    refresh
  };
}
