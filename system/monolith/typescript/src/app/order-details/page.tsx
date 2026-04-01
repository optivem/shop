"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

interface OrderDetail {
  orderNumber: string;
  orderTimestamp: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  status: string;
}

function OrderDetailsContent() {
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get("orderNumber");

  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!orderNumber) {
      setLoading(false);
      setError("No order number specified");
      return;
    }

    async function loadOrder() {
      try {
        const response = await fetch(`/api/orders/${encodeURIComponent(orderNumber!)}`);

        if (response.status === 404) {
          setError("Order not found");
          setLoading(false);
          return;
        }

        if (!response.ok) {
          const data = await response.json();
          setError(data.detail || "Failed to load order");
          setLoading(false);
          return;
        }

        const data = await response.json();
        setOrder(data);
      } catch (err) {
        setError(`Network error: ${err instanceof Error ? err.message : String(err)}`);
      } finally {
        setLoading(false);
      }
    }

    loadOrder();
  }, [orderNumber]);

  return (
    <main>
      <nav>
        <Link href="/">Home</Link> &gt; <Link href="/order-history">Order History</Link> &gt; Order Details
      </nav>

      <h1>Order Details</h1>

      {error && (
        <div role="alert" style={{ color: "red", marginBottom: "1rem" }}>
          {error}
        </div>
      )}

      {loading && <p>Loading order details...</p>}

      {!loading && order && (
        <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: "1rem", maxWidth: "600px" }}>
          <div>
            <strong>Order Number</strong>
            <p aria-label="Display Order Number">{order.orderNumber}</p>
          </div>
          <div>
            <strong>Order Timestamp</strong>
            <p aria-label="Display Order Timestamp">
              {new Date(order.orderTimestamp).toLocaleString("en-US", { timeZone: "UTC" })}
            </p>
          </div>
          <div>
            <strong>Status</strong>
            <p aria-label="Display Status">{order.status}</p>
          </div>
          <div>
            <strong>SKU</strong>
            <p aria-label="Display SKU">{order.sku}</p>
          </div>
          <div>
            <strong>Quantity</strong>
            <p aria-label="Display Quantity">{order.quantity}</p>
          </div>
          <div>
            <strong>Unit Price</strong>
            <p aria-label="Display Unit Price">${order.unitPrice.toFixed(2)}</p>
          </div>
          <div>
            <strong>Total Price</strong>
            <p aria-label="Display Total Price">${order.totalPrice.toFixed(2)}</p>
          </div>
          <div style={{ gridColumn: "1 / -1", marginTop: "1rem" }}>
            <Link href="/order-history">Back to Order History</Link>
          </div>
        </div>
      )}
    </main>
  );
}

export default function OrderDetailsPage() {
  return (
    <Suspense fallback={<main><p>Loading...</p></main>}>
      <OrderDetailsContent />
    </Suspense>
  );
}
