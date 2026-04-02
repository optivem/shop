"use client";

import { useState } from "react";

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
    fieldErrors: string[];
    id: number;
  } | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [notificationCounter, setNotificationCounter] = useState(0);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setSubmitting(true);
    setNotification(null);

    const nextId = notificationCounter + 1;
    setNotificationCounter(nextId);

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
          message: `Success! Order has been created with Order Number ${data.orderNumber}`,
          fieldErrors: [],
          id: nextId,
        });
      } else {
        const errorData = data as ErrorData;
        const fieldErrors: string[] = [];
        if (errorData.errors && errorData.errors.length > 0) {
          errorData.errors.forEach((err) => {
            const fieldPart = err.field ? `${err.field}: ` : "";
            fieldErrors.push(`${fieldPart}${err.message}`);
          });
        }
        setNotification({
          type: "error",
          message: errorData.detail || "An error occurred",
          fieldErrors,
          id: nextId,
        });
      }
    } catch (err) {
      setNotification({
        type: "error",
        message: `Network error: ${err instanceof Error ? err.message : String(err)}`,
        fieldErrors: [],
        id: nextId,
      });
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <a href="/">Home</a>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Shop
          </li>
        </ol>
      </nav>

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
                  {fe}
                </div>
              ))}
            </>
          )}
        </div>
      )}

      <div className="row">
        <div className="col-lg-6 mx-auto">
          <div className="card shadow">
            <div className="card-header bg-primary text-white">
              <h4 className="mb-0">Place Your Order</h4>
            </div>
            <div className="card-body">
              <form onSubmit={handleSubmit}>
                <div className="mb-3">
                  <label htmlFor="sku" className="form-label">
                    SKU:
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    id="sku"
                    value={sku}
                    onChange={(e) => setSku(e.target.value)}
                    placeholder="Enter product SKU"
                    aria-label="SKU"
                  />
                </div>
                <div className="mb-3">
                  <label htmlFor="quantity" className="form-label">
                    Quantity:
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    id="quantity"
                    inputMode="numeric"
                    value={quantity}
                    onChange={(e) => setQuantity(e.target.value)}
                    placeholder="Enter quantity"
                    aria-label="Quantity"
                  />
                </div>
                <div className="d-grid">
                  <button
                    type="submit"
                    className="btn btn-primary btn-lg"
                    disabled={submitting}
                    aria-label="Place Order"
                  >
                    {submitting ? "Placing Order..." : "Place Order"}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
