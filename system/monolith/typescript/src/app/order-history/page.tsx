"use client";

import { useState, useEffect, useCallback, useRef } from "react";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { AccountLayout, ErrorAlert, LoadingSpinner } from "@/components/StoreLayout";
import { useSession } from "@/components/StorefrontProviders";
import { customersResponseSchema, ordersResponseSchema, parseJson, readErrorData, type Order } from "@/lib/api-types";

function ordersUrl(params: { orderNumber?: string; customer?: string }): string {
  const query = new URLSearchParams();
  if (params.orderNumber) query.set("orderNumber", params.orderNumber);
  if (params.customer) query.set("customer", params.customer);
  const search = query.toString();
  return search ? `/api/orders?${search}` : "/api/orders";
}

const withCustomer = (known: string[], chosen: string) => [...known, chosen].sort((a, b) => a.localeCompare(b));

/** The customers who have placed orders, A to Z, from the admin customer list (UC-011). */
async function fetchCustomers(): Promise<string[] | null> {
  const response = await fetch("/api/admin/customers");
  return response.ok ? parseJson(response, customersResponseSchema) : null;
}

/**
 * Order History page: My orders for a customer, Admin › All orders for an admin (UC-002, UC-011).
 * Filters by order number; an admin can also filter by the customer who placed the order.
 */
export default function OrderHistoryPage() {
  const isAdmin = useSession()?.isAdmin ?? false;
  const [filter, setFilter] = useState("");
  const [customer, setCustomer] = useState("");
  const [customers, setCustomers] = useState<string[]>([]);
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Only the latest reload renders: an older response that lands after a newer request is dropped.
  const latestOrdersRequest = useRef(0);

  const loadOrders = useCallback(async (orderNumberFilter?: string, customerFilter?: string) => {
    const request = ++latestOrdersRequest.current;
    const isLatest = () => request === latestOrdersRequest.current;
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(ordersUrl({ orderNumber: orderNumberFilter, customer: customerFilter }));

      if (!response.ok) {
        const data = await readErrorData(response);
        if (!isLatest()) return;
        setError(data.detail ?? "Failed to load orders");
        setOrders([]);
        return;
      }

      const data = await parseJson(response, ordersResponseSchema);
      if (!isLatest()) return;
      setOrders(data.orders);
    } catch (err) {
      if (!isLatest()) return;
      setError(
        `Network error: ${err instanceof Error ? err.message : String(err)}`
      );
      setOrders([]);
    } finally {
      if (isLatest()) setLoading(false);
    }
  }, []);

  // Admins see who placed each order (UC-011); the API reports the customer only to admins.
  const showCustomer = isAdmin || orders.some((order) => order.customer !== undefined);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  // The customers for the filter come from their own admin endpoint, fetched once and on their own, so a newer
  // filter never drops them.
  useEffect(() => {
    if (!isAdmin) return;
    let ignore = false;
    void fetchCustomers()
      .then((found) => {
        if (!ignore && found) setCustomers(found);
      })
      .catch(() => undefined);
    return () => {
      ignore = true;
    };
  }, [isAdmin]);

  function handleRefresh() {
    void loadOrders(filter || undefined, customer || undefined);
  }

  function handleCustomerChange(value: string) {
    setCustomer(value);
    void loadOrders(filter || undefined, value || undefined);
  }

  // The chosen customer stays offered even before the list of customers has arrived.
  const customerOptions = customer && !customers.includes(customer) ? withCustomer(customers, customer) : customers;

  const columnCount = showCustomer ? 8 : 7;

  return (
    <AccountLayout
      title={isAdmin ? "All orders" : "My orders"}
      breadcrumbs={[{ label: "Home", path: "/" }, ...(isAdmin ? [{ label: "Admin" }, { label: "All orders" }] : [{ label: "My orders" }])]}
    >
      <div className="orders-toolbar">
        <div>
          <h1 className="h2 mb-1">Order History</h1>
          {!loading && !error && (
            <p className="text-body-secondary mb-0">
              {orders.length} {orders.length === 1 ? "order" : "orders"}
            </p>
          )}
        </div>
        <div className="orders-filters">
          {isAdmin && (
            <div className="orders-filter">
              <label htmlFor="customerFilter" className="form-label market-label">
                Customer:
              </label>
              <select
                id="customerFilter"
                className="form-select"
                aria-label="Customer Filter"
                value={customer}
                onChange={(e) => handleCustomerChange(e.target.value)}
              >
                <option value="">All customers</option>
                {customerOptions.map((option) => (
                  <option key={option} value={option}>
                    {option}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="orders-filter orders-filter-wide">
            <label htmlFor="orderNumberFilter" className="form-label market-label">
              Filter by Order Number:
            </label>
            <input
              type="text"
              className="form-control"
              id="orderNumberFilter"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") handleRefresh();
              }}
              placeholder="Enter order number..."
              aria-label="Order Number"
            />
          </div>
          <button
            type="button"
            className="btn btn-outline-secondary orders-refresh"
            onClick={handleRefresh}
            disabled={loading}
            aria-label="Refresh Order List"
          >
            <Icon name="refresh-cw" />
            Refresh
          </button>
        </div>
      </div>

      {loading && (
        <div className="market-panel">
          <LoadingSpinner message="Loading orders..." />
        </div>
      )}
      {!loading && error && <ErrorAlert message={error} onRetry={handleRefresh} />}
      {!loading && !error && (
        <div className="market-panel orders-panel">
          {/* The table keeps all its columns: on small screens the panel scrolls sideways, the page never does. */}
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0 orders-table">
              <thead>
                <tr>
                  <th>Order Number</th>
                  {showCustomer && <th>Customer</th>}
                  <th>Order Date</th>
                  <th>SKU</th>
                  <th>Quantity</th>
                  <th>Total Price</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {orders.length === 0 ? (
                  <tr>
                    <td colSpan={columnCount} className="orders-empty text-center">
                      No orders found
                    </td>
                  </tr>
                ) : (
                  orders.map((order) => (
                    <tr key={order.orderNumber}>
                      <td className="orders-cell-orderNumber">
                        <span className="order-number">{order.orderNumber}</span>
                      </td>
                      {showCustomer && <td className="orders-cell-customer">{order.customer ?? "-"}</td>}
                      <td className="orders-cell-orderTimestamp">
                        {new Date(order.orderTimestamp).toLocaleString("en-US", {
                          timeZone: "UTC",
                        })}
                      </td>
                      <td className="orders-cell-sku">{order.sku}</td>
                      <td className="orders-cell-quantity">{order.quantity}</td>
                      <td className="orders-cell-totalPrice">
                        <span className="order-total">${order.totalPrice}</span>
                      </td>
                      <td className="orders-cell-status">
                        <span className={`order-status status-${order.status}`}>{order.status}</span>
                      </td>
                      <td className="orders-cell-actions">
                        {/* "View Details" must stay the link's first text node: drivers find it with contains(text(), ...). */}
                        <Link
                          href={`/order-details/${encodeURIComponent(order.orderNumber)}`}
                          className="order-view-link"
                        >
                          View Details
                          <Icon name="chevron-right" />
                        </Link>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </AccountLayout>
  );
}
