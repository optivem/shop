import { useState, useEffect, useCallback } from 'react';
import { orderService } from '../services/order-service';
import type { ViewOrderDetailsResponse } from '../types/api.types';

interface OrderRequest {
  orderNumber: string;
  reloadCount: number;
}

/**
 * Custom hook for managing order details
 * @param orderNumber - The order number to fetch details for
 * @returns Order details state, loading states, and control functions
 */
export function useOrderDetails(orderNumber: string | undefined) {
  const [order, setOrder] = useState<ViewOrderDetailsResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);
  // The request whose response is currently applied; loading is derived from it.
  const [loaded, setLoaded] = useState<OrderRequest | null>(null);

  useEffect(() => {
    if (!orderNumber) return;
    // Ignore the response if orderNumber changes, refresh() is called again, or the
    // component unmounts before it arrives — only the latest request may win.
    let ignore = false;
    void orderService.getOrder(orderNumber).then((result) => {
      if (ignore) return;
      if (result.success) {
        setOrder(result.data);
        setError(null);
      } else {
        setError(result.error.message);
      }
      setLoaded({ orderNumber, reloadCount });
    });
    return () => {
      ignore = true;
    };
  }, [orderNumber, reloadCount]);

  const refresh = useCallback(() => {
    setReloadCount((count) => count + 1);
  }, []);

  if (!orderNumber) {
    return { order, isLoading: false, error: 'No order number provided', refresh };
  }

  const isLoading = loaded?.orderNumber !== orderNumber || loaded.reloadCount !== reloadCount;

  return { order, isLoading, error, refresh };
}
