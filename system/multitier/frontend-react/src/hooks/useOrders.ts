import { useState, useEffect, useCallback } from 'react';
import { orderService } from '../services/order-service';
import type { BrowseOrderHistoryItemResponse } from '../types/api.types';

interface OrdersRequest {
  filter: string;
  reloadCount: number;
}

/**
 * Custom hook for managing order history browsing and filtering
 * @param initialFilter - Optional initial filter value for order number
 * @returns Order history state and control functions
 */
export function useOrders(initialFilter = '') {
  const [orders, setOrders] = useState<BrowseOrderHistoryItemResponse[]>([]);
  const [filter, setFilter] = useState(initialFilter);
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);
  // The request whose response is currently applied; loading is derived from it.
  const [loaded, setLoaded] = useState<OrdersRequest | null>(null);

  useEffect(() => {
    // The filter is typed keystroke by keystroke, so responses can arrive out of order:
    // ignore any response superseded by a newer filter, refresh, or unmount.
    let ignore = false;
    void orderService.browseOrderHistory(filter).then((result) => {
      if (ignore) return;
      if (result.success) {
        setOrders(result.data.orders);
        setError(null);
      } else {
        setError(result.error.message);
      }
      setLoaded({ filter, reloadCount });
    });
    return () => {
      ignore = true;
    };
  }, [filter, reloadCount]);

  const refresh = useCallback(() => {
    setReloadCount((count) => count + 1);
  }, []);

  const isLoading = loaded?.filter !== filter || loaded.reloadCount !== reloadCount;

  return {
    orders,
    filter,
    setFilter,
    isLoading,
    error,
    refresh
  };
}
