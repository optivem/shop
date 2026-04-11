"use client";

import { useState, useEffect, Suspense } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";

interface OrderDetail {
  orderNumber: string;
  orderTimestamp: string;
  country: string;
  sku: string;
  quantity: number;
  unitPrice: number;
  basePrice: number;
  discountRate: number;
  discountAmount: number;
  subtotalPrice: number;
  taxRate: number;
  taxAmount: number;
  totalPrice: number;
  appliedCouponCode: string | null;
  status: string;
}

function OrderDetailsContent() {
  const searchParams = useSearchParams();
  const orderNumber = searchParams.get("orderNumber");

  const [order, setOrder] = useState<OrderDetail | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCancelling, setIsCancelling] = useState(false);
  const [isDelivering, setIsDelivering] = useState(false);
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
    fieldErrors: string[];
    id: number;
  } | null>(null);
  const [notificationCounter, setNotificationCounter] = useState(0);

  useEffect(() => {
    if (!orderNumber) {
      setLoading(false);
      setError("No order number specified");
      return;
    }

    async function loadOrder() {
      try {
        const response = await fetch(
          `/api/orders/${encodeURIComponent(orderNumber!)}`
        );

        if (response.status === 404) {
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
        setError(
          `Network error: ${err instanceof Error ? err.message : String(err)}`
        );
      } finally {
        setLoading(false);
      }
    }

    loadOrder();
  }, [orderNumber]);

  async function handleCancel() {
    if (!orderNumber) return;
    setIsCancelling(true);
    setNotification(null);

    const nextId = notificationCounter + 1;
    setNotificationCounter(nextId);

    try {
      const response = await fetch(
        `/api/orders/${encodeURIComponent(orderNumber)}/cancel`,
        { method: "POST" }
      );
      if (response.ok) {
        setOrder((prev) => (prev ? { ...prev, status: "CANCELLED" } : prev));
        setNotification({
          type: "success",
          message: "Order has been cancelled successfully",
          fieldErrors: [],
          id: nextId,
        });
      } else {
        const data = await response.json();
        const fieldErrors: string[] = [];
        if (data.errors && data.errors.length > 0) {
          data.errors.forEach((err: { field?: string; message: string }) => {
            const fieldPart = err.field ? `${err.field}: ` : "";
            fieldErrors.push(`${fieldPart}${err.message}`);
          });
        }
        setNotification({
          type: "error",
          message: data.detail || "Failed to cancel order",
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
      setIsCancelling(false);
    }
  }

  async function handleDeliver() {
    if (!orderNumber) return;
    setIsDelivering(true);
    setNotification(null);

    const nextId = notificationCounter + 1;
    setNotificationCounter(nextId);

    try {
      const response = await fetch(
        `/api/orders/${encodeURIComponent(orderNumber)}/deliver`,
        { method: "POST" }
      );
      if (response.ok) {
        setOrder((prev) => (prev ? { ...prev, status: "DELIVERED" } : prev));
        setNotification({
          type: "success",
          message: "Order has been delivered successfully",
          fieldErrors: [],
          id: nextId,
        });
      } else {
        const data = await response.json();
        const fieldErrors: string[] = [];
        if (data.errors && data.errors.length > 0) {
          data.errors.forEach((err: { field?: string; message: string }) => {
            const fieldPart = err.field ? `${err.field}: ` : "";
            fieldErrors.push(`${fieldPart}${err.message}`);
          });
        }
        setNotification({
          type: "error",
          message: data.detail || "Failed to deliver order",
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
      setIsDelivering(false);
    }
  }

  return (
    <>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Home</Link>
          </li>
          <li className="breadcrumb-item">
            <Link href="/order-history">Order History</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Order Details
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
              {notification.fieldErrors.map((fe) => (
                <div key={fe} className="field-error">
                  {fe}
                </div>
              ))}
            </>
          )}
        </div>
      )}

      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Order Details</h4>
        </div>
        <div className="card-body">
          {loading && (
            <div className="text-center py-5">
              <output>
                <div className="spinner-border text-primary">
                  <span className="visually-hidden">Loading...</span>
                </div>
              </output>
              <p className="mt-3">Loading order details...</p>
            </div>
          )}
          {!loading && error && !notification && (
            <div
              className="notification error"
              role="alert"
              data-notification-id={0}
            >
              <div className="error-message">{error}</div>
            </div>
          )}
          {!loading && !error && !order && (
            <div className="text-center py-5 text-muted">
              <p>Order not found</p>
            </div>
          )}
          {!loading && !error && order && (
            <>
              <div className="row">
                <div className="col-md-6 mb-3">
                  <strong>Order Number:</strong>
                  <p aria-label="Display Order Number">{order.orderNumber}</p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Order Timestamp:</strong>
                  <p aria-label="Display Order Timestamp">
                    {new Date(order.orderTimestamp).toLocaleString("en-US", {
                      timeZone: "UTC",
                    })}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Status:</strong>
                  <p
                    className={`status-${order.status}`}
                    aria-label="Display Status"
                  >
                    {order.status}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>SKU:</strong>
                  <p aria-label="Display SKU">{order.sku}</p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Country:</strong>
                  <p aria-label="Display Country">{order.country}</p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Quantity:</strong>
                  <p aria-label="Display Quantity">{order.quantity}</p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Unit Price:</strong>
                  <p aria-label="Display Unit Price">
                    ${order.unitPrice.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Base Price:</strong>
                  <p aria-label="Display Base Price">
                    ${order.basePrice.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Discount Rate:</strong>
                  <p aria-label="Display Discount Rate">
                    {(order.discountRate * 100).toFixed(2)}%
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Discount Amount:</strong>
                  <p aria-label="Display Discount Amount">
                    ${order.discountAmount.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Subtotal Price:</strong>
                  <p aria-label="Display Subtotal Price">
                    ${order.subtotalPrice.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Tax Rate:</strong>
                  <p aria-label="Display Tax Rate">
                    {(order.taxRate * 100).toFixed(2)}%
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Tax Amount:</strong>
                  <p aria-label="Display Tax Amount">
                    ${order.taxAmount.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Total Price:</strong>
                  <p
                    className="fs-5 fw-bold"
                    aria-label="Display Total Price"
                  >
                    ${order.totalPrice.toFixed(2)}
                  </p>
                </div>
                <div className="col-md-6 mb-3">
                  <strong>Applied Coupon Code:</strong>
                  <p aria-label="Display Applied Coupon">
                    {order.appliedCouponCode || 'None'}
                  </p>
                </div>
              </div>
              <div className="mt-4 d-flex gap-2">
                {order.status !== "CANCELLED" && order.status !== "DELIVERED" && (
                  <button
                    className="btn btn-danger"
                    aria-label="Cancel Order"
                    onClick={handleCancel}
                    disabled={isCancelling}
                  >
                    {isCancelling ? "Cancelling..." : "Cancel Order"}
                  </button>
                )}
                {order.status !== "CANCELLED" && order.status !== "DELIVERED" && (
                  <button
                    className="btn btn-warning"
                    aria-label="Deliver Order"
                    onClick={handleDeliver}
                    disabled={isDelivering}
                  >
                    {isDelivering ? "Delivering..." : "Deliver Order"}
                  </button>
                )}
                <Link href="/order-history" className="btn btn-secondary">
                  Back to Order History
                </Link>
              </div>
            </>
          )}
        </div>
      </div>
    </>
  );
}

export default function OrderDetailsPage() {
  return (
    <Suspense
      fallback={
        <div className="text-center py-5">
          <output>
            <div className="spinner-border text-primary">
              <span className="visually-hidden">Loading...</span>
            </div>
          </output>
          <p className="mt-3">Loading...</p>
        </div>
      }
    >
      <OrderDetailsContent />
    </Suspense>
  );
}
