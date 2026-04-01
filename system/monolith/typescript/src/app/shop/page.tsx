"use client";

import { useState } from "react";
import Link from "next/link";

interface FieldError {
  field: string;
  message: string;
}

interface ErrorData {
  detail?: string;
  errors?: FieldError[];
}

export default function ShopPage() {
  const [sku, setSku] = useState("");
  const [quantity, setQuantity] = useState("");
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
    fieldErrors: FieldError[];
    id: string;
  } | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setNotification(null);

    try {
      const body: Record<string, unknown> = {};
      if (sku !== "") body.sku = sku;
      if (quantity !== "") {
        const parsed = parseInt(quantity);
        body.quantity = isNaN(parsed) ? quantity : parsed;
      }

      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const data = await response.json();

      if (response.ok) {
        setNotification({
          type: "success",
          message: `Order has been created with Order Number ${data.orderNumber}`,
          fieldErrors: [],
          id: crypto.randomUUID(),
        });
      } else {
        const errorData = data as ErrorData;
        setNotification({
          type: "error",
          message: errorData.detail || "An error occurred",
          fieldErrors: errorData.errors || [],
          id: crypto.randomUUID(),
        });
      }
    } catch (err) {
      setNotification({
        type: "error",
        message: `Network error: ${err instanceof Error ? err.message : String(err)}`,
        fieldErrors: [],
        id: crypto.randomUUID(),
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <main>
      <nav>
        <Link href="/">Home</Link> &gt; Shop
      </nav>

      <h1>Shop</h1>

      {notification && (
        <div
          role="alert"
          className={`notification ${notification.type}`}
          data-notification-id={notification.id}
        >
          {notification.type === "success" ? (
            notification.message
          ) : (
            <>
              <div className="error-message">{notification.message}</div>
              {notification.fieldErrors.map((fe, i) => (
                <div key={i} className="field-error">
                  {fe.field}: {fe.message}
                </div>
              ))}
            </>
          )}
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ maxWidth: "400px", marginTop: "1rem" }}>
        <div style={{ marginBottom: "1rem" }}>
          <label htmlFor="sku">SKU</label>
          <br />
          <input
            type="text"
            id="sku"
            value={sku}
            onChange={(e) => setSku(e.target.value)}
            placeholder="Enter product SKU"
            aria-label="SKU"
            style={{ width: "100%", padding: "0.5rem" }}
          />
        </div>
        <div style={{ marginBottom: "1rem" }}>
          <label htmlFor="quantity">Quantity</label>
          <br />
          <input
            type="text"
            id="quantity"
            inputMode="numeric"
            value={quantity}
            onChange={(e) => setQuantity(e.target.value)}
            placeholder="Enter quantity"
            aria-label="Quantity"
            style={{ width: "100%", padding: "0.5rem" }}
          />
        </div>
        <button
          type="submit"
          disabled={submitting}
          aria-label="Place Order"
          style={{ padding: "0.5rem 1.5rem", cursor: "pointer" }}
        >
          {submitting ? "Placing Order..." : "Place Order"}
        </button>
      </form>
    </main>
  );
}
