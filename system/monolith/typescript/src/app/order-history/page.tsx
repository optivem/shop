"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { AccountLayout, ErrorAlert, LoadingSpinner } from "@/components/StoreLayout";
import { useSession } from "@/components/StorefrontProviders";
import { ordersResponseSchema, parseJson, readErrorData, type Order } from "@/lib/api-types";

// The admin list is paged; the customer filter offers the customers found on its pages, up to this many.
const MAX_CUSTOMER_PAGES = 50;

function ordersUrl(params: { orderNumber?: string; customer?: string; page?: number }): string {
  const query = new URLSearchParams();
  if (params.orderNumber) query.set("orderNumber", params.orderNumber);
  if (params.customer) query.set("customer", params.customer);
  if (params.page !== undefined) query.set("page", String(params.page));
  const search = query.toString();
  return search ? `/api/orders?${search}` : "/api/orders";
}

const mergeCustomers = (known: string[], more: string[]) => [...new Set([...known, ...more])].sort((a, b) => a.localeCompare(b));

/** The customers who have placed orders, from every page of the unfiltered admin list. */
async function collectAllCustomers(): Promise<string[]> {
  const customers: string[] = [];
  let totalPages = 1;
  for (let page = 1; page <= Math.min(totalPages, MAX_CUSTOMER_PAGES); page++) {
    const response = await fetch(ordersUrl({ page }));
    if (!response.ok) break;
    const data = await parseJson(response, ordersResponseSchema);
    customers.push(...data.orders.map((order) => order.customer).filter((customer): customer is string => !!customer));
    totalPages = data.totalPages ?? 1;
  }
  return customers;
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

  const loadOrders = useCallback(async (orderNumberFilter?: string, customerFilter?: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await fetch(ordersUrl({ orderNumber: orderNumberFilter, customer: customerFilter }));

      if (!response.ok) {
        const data = await readErrorData(response);
        setError(data.detail ?? "Failed to load orders");
        setOrders([]);
        return;
      }

      const data = await parseJson(response, ordersResponseSchema);
      setOrders(data.orders);
    } catch (err) {
      setError(
        `Network error: ${err instanceof Error ? err.message : String(err)}`
      );
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, []);

  // Admins see who placed each order (UC-011); the API reports the customer only to admins.
  const showCustomer = isAdmin || orders.some((order) => order.customer !== undefined);

  useEffect(() => {
    void loadOrders();
  }, [loadOrders]);

  useEffect(() => {
    if (!isAdmin) return;
    let ignore = false;
    void collectAllCustomers()
      .then((found) => {
        if (!ignore) setCustomers((known) => mergeCustomers(known, found));
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
  const customerOptions = customer && !customers.includes(customer) ? mergeCustomers(customers, [customer]) : customers;

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
                      {showCustomer && <td>{order.customer ?? "-"}</td>}
                      <td className="orders-cell-orderTimestamp">
                        {new Date(order.orderTimestamp).toLocaleString("en-US", {
                          timeZone: "UTC",
                        })}
                      </td>
                      <td className="orders-cell-sku">{order.sku}</td>
                      <td>{order.quantity}</td>
                      <td className="orders-cell-totalPrice">
                        <span className="order-total">${order.totalPrice}</span>
                      </td>
                      <td>
                        <span className={`order-status status-${order.status}`}>{order.status}</span>
                      </td>
                      <td className="orders-cell-actions">
                        {/* "View Details" must stay the link's first text node: drivers find it with contains(text(), ...). */}
                        <Link
                          href={`/order-details?orderNumber=${encodeURIComponent(order.orderNumber)}`}
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
