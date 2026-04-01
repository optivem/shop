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
      setError(`Network error: ${err instanceof Error ? err.message : String(err)}`);
      setOrders([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadOrders();
  }, [loadOrders]);

  function handleSearch() {
    loadOrders(filter || undefined);
  }

  return (
    <main>
      <nav>
        <Link href="/">Home</Link> &gt; Order History
      </nav>

      <h1>Order History</h1>

      {error && (
        <div role="alert" style={{ color: "red", marginBottom: "1rem" }}>
          {error}
        </div>
      )}

      <div style={{ display: "flex", gap: "0.5rem", marginBottom: "1rem" }}>
        <input
          type="text"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter") handleSearch();
          }}
          placeholder="Filter by order number..."
          aria-label="Order Number"
          style={{ flex: 1, padding: "0.5rem" }}
        />
        <button
          onClick={handleSearch}
          aria-label="Refresh Order List"
          style={{ padding: "0.5rem 1rem", cursor: "pointer" }}
        >
          Search
        </button>
      </div>

      {loading && <p>Loading orders...</p>}

      {!loading && orders.length === 0 && !error && <p>No orders found</p>}

      {!loading && orders.length > 0 && (
        <table style={{ width: "100%", borderCollapse: "collapse" }}>
          <thead>
            <tr>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Order Number</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Timestamp</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>SKU</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Quantity</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Total Price</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Status</th>
              <th style={{ textAlign: "left", borderBottom: "2px solid #ddd", padding: "0.5rem" }}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {orders.map((order) => (
              <tr key={order.orderNumber}>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>{order.orderNumber}</td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>
                  {new Date(order.orderTimestamp).toLocaleString("en-US", { timeZone: "UTC" })}
                </td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>{order.sku}</td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>{order.quantity}</td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>${order.totalPrice.toFixed(2)}</td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>{order.status}</td>
                <td style={{ borderBottom: "1px solid #eee", padding: "0.5rem" }}>
                  <Link href={`/order-details?orderNumber=${encodeURIComponent(order.orderNumber)}`}>
                    View Details
                  </Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </main>
  );
}
