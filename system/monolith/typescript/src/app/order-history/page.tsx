"use client";

import { useState, useEffect, useCallback } from "react";
import Link from "next/link";

interface Order {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  totalPrice: number;
  status: string;
}

export default function OrderHistoryPage() {
  const [filter, setFilter] = useState("");
  const [orders, setOrders] = useState<Order[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadOrders = useCallback(async (orderNumberFilter?: string) => {
    setLoading(true);
    setError(null);

    try {
      let url = "/api/orders";
      if (orderNumberFilter) {
        url += `?orderNumber=${encodeURIComponent(orderNumberFilter)}`;
      }

      const response = await fetch(url);
      const data = await response.json();

      if (!response.ok) {
        setError(data.detail || "Failed to load orders");
        setOrders([]);
        return;
      }

      setOrders(data.orders || []);
    } catch (err) {
      setError(
        `Network error: ${err instanceof Error ? err.message : String(err)}`
      );
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  function handleRefresh() {
    loadOrders(filter || undefined);
  }

  return (
    <>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Home</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Order History
          </li>
        </ol>
      </nav>

      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Order History</h4>
        </div>
        <div className="card-body">
          <div className="row mb-3">
            <div className="col-md-8">
              <label htmlFor="orderNumberFilter" className="form-label">
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
            <div className="col-md-4 d-flex align-items-end">
              <button
                className="btn btn-secondary w-100"
                onClick={handleRefresh}
                disabled={loading}
                aria-label="Refresh Order List"
              >
                Refresh
              </button>
            </div>
          </div>

          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <p className="mt-3">Loading orders...</p>
            </div>
          ) : error ? (
            <div
              className="alert alert-danger d-flex justify-content-between align-items-center"
              role="alert"
            >
              <div>
                <strong>Error:</strong> {error}
              </div>
              <button
                className="btn btn-sm btn-outline-danger"
                onClick={handleRefresh}
              >
                Try Again
              </button>
            </div>
          ) : orders.length === 0 ? (
            <div className="table-responsive">
              <table className="table table-striped table-hover">
                <thead>
                  <tr>
                    <th>Order Number</th>
                    <th>Order Date</th>
                    <th>SKU</th>
                    <th>Quantity</th>
                    <th>Total Price</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td colSpan={7} className="text-center">
                      No orders found
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-striped table-hover">
                <thead>
                  <tr>
                    <th>Order Number</th>
                    <th>Order Date</th>
                    <th>SKU</th>
                    <th>Quantity</th>
                    <th>Total Price</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.orderNumber}>
                      <td>{order.orderNumber}</td>
                      <td>
                        {new Date(order.orderTimestamp).toLocaleString("en-US", {
                          timeZone: "UTC",
                        })}
                      </td>
                      <td>{order.sku}</td>
                      <td>{order.quantity}</td>
                      <td>${order.totalPrice.toFixed(2)}</td>
                      <td>
                        <span className={`status-${order.status}`}>
                          {order.status}
                        </span>
                      </td>
                      <td>
                        <Link
                          href={`/order-details?orderNumber=${encodeURIComponent(order.orderNumber)}`}
                        >
                          View Details
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
