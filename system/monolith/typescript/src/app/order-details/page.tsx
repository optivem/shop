"use client";

import { useState, useEffect, Suspense, type ReactNode } from "react";
import { useSearchParams } from "next/navigation";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { AccountLayout, LoadingSpinner } from "@/components/StoreLayout";
import { ProductImage } from "@/components/StorefrontParts";
import { useCatalog } from "@/components/StorefrontProviders";
import { formatAddress } from "@/lib/storefront/shipping";
import {
  formatFieldErrors,
  orderDetailSchema,
  parseJson,
  readErrorData,
  type OrderDetail,
} from "@/lib/api-types";

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

    async function loadOrder(orderNumberToLoad: string) {
      try {
        const response = await fetch(
          `/api/orders/${encodeURIComponent(orderNumberToLoad)}`
        );

        if (response.status === 404) {
          setLoading(false);
          return;
        }

        if (!response.ok) {
          const data = await readErrorData(response);
          setError(data.detail ?? "Failed to load order");
          setLoading(false);
          return;
        }

        setOrder(await parseJson(response, orderDetailSchema));
      } catch (err) {
        setError(
          `Network error: ${err instanceof Error ? err.message : String(err)}`
        );
      } finally {
        setLoading(false);
      }
    }

    void loadOrder(orderNumber);
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
        const data = await readErrorData(response);
        const fieldErrors = formatFieldErrors(data);
        setNotification({
          type: "error",
          message: data.detail ?? "Failed to cancel order",
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
        const data = await readErrorData(response);
        const fieldErrors = formatFieldErrors(data);
        setNotification({
          type: "error",
          message: data.detail ?? "Failed to deliver order",
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
    <div className="order-detail">
      {notification && (
        <div
          role="alert"
          className={`notification ${notification.type}`}
          data-notification-id={notification.id}
        >
          {notification.type === "success" ? (
            <>
              <Icon name="circle-check" size={20} className="notification-icon" />
              {notification.message}
            </>
          ) : (
            <>
              <Icon name="circle-alert" size={20} className="notification-icon" />
              <div className="notification-body">
                <div className="error-message">{notification.message}</div>
                {notification.fieldErrors.map((fe) => (
                  <div key={fe} className="field-error">
                    {fe}
                  </div>
                ))}
              </div>
            </>
          )}
        </div>
      )}

      <Link href="/order-history" className="btn btn-link order-back">
        <Icon name="arrow-left" />
        Back to Order History
      </Link>
      {loading && (
        <div className="market-panel">
          <LoadingSpinner message="Loading order details..." />
        </div>
      )}
      {!loading && error && !notification && (
        <div className="notification error" role="alert" data-notification-id={0}>
          <Icon name="circle-alert" size={20} className="notification-icon" />
          <div className="notification-body">
            <div className="error-message">{error}</div>
          </div>
        </div>
      )}
      {!loading && !error && !order && (
        <div className="market-empty text-center py-5 px-3">
          <p className="mb-0">Order not found</p>
        </div>
      )}
      {!loading && !error && order && <OrderDetailView order={order} actions={
        order.status !== "CANCELLED" && order.status !== "DELIVERED" && (
          <>
            <button
              type="button"
              className="btn btn-outline-danger"
              aria-label="Cancel Order"
              onClick={() => {
                void handleCancel();
              }}
              disabled={isCancelling}
            >
              {isCancelling ? "Cancelling..." : "Cancel Order"}
            </button>
            <button
              type="button"
              className="btn btn-primary"
              aria-label="Deliver Order"
              onClick={() => {
                void handleDeliver();
              }}
              disabled={isDelivering}
            >
              {isDelivering ? "Delivering..." : "Deliver Order"}
            </button>
          </>
        )
      } />}
    </div>
  );
}

const percent = (rate: string) => `${(Number(rate) * 100).toFixed(2)}%`;

/**
 * A header with the order number, date and status, the ordered item, and the price breakdown.
 * Every value the drivers read sits alone in its `Display <Field>` element, exactly as the API returned it.
 */
function OrderDetailView({ order, actions }: Readonly<{ order: OrderDetail; actions: ReactNode }>) {
  // The order only carries the SKU; the catalog (when loaded) gives the title and brand for the item row.
  const product = useCatalog().products.find((candidate) => candidate.sku === order.sku);
  const timestamp = new Date(order.orderTimestamp).toLocaleString("en-US", { timeZone: "UTC" });

  return (
    <>
      <section className="market-panel order-detail-header">
        <div className="min-w-0">
          <p className="order-eyebrow mb-1">Order Details</p>
          <h1 className="order-detail-number">
            <span aria-label="Display Order Number">{order.orderNumber}</span>
          </h1>
          <p className="text-body-secondary mb-0 d-flex flex-wrap align-items-center gap-2">
            <span>
              Ordered <span aria-label="Display Order Timestamp">{timestamp}</span> UTC
            </span>
            <span className={`order-status status-${order.status}`} aria-label="Display Status">
              {order.status}
            </span>
          </p>
        </div>
        {actions && <div className="order-detail-actions">{actions}</div>}
      </section>

      <div className="row g-4 align-items-start">
        <div className="col-xl-7">
          <section className="market-panel p-4" aria-labelledby="order-item-heading">
            <h2 className="h5 mb-3" id="order-item-heading">
              Item
            </h2>
            <div className="order-item">
              <div className="order-item-thumb">
                <ProductImage brand={product?.brand ?? order.sku} shape="square" />
              </div>
              <div className="min-w-0">
                {product && <p className="order-item-title mb-0">{product.title}</p>}
                <p className="order-item-meta mb-0">
                  SKU{" "}
                  <span className="font-monospace" aria-label="Display SKU">
                    {order.sku}
                  </span>{" "}
                  · Quantity <span aria-label="Display Quantity">{order.quantity}</span> · Unit Price{" "}
                  <span aria-label="Display Unit Price">${order.unitPrice}</span>
                </p>
              </div>
            </div>
            <dl className="order-facts">
              {order.shippingAddress && (
                <div>
                  <dt>Shipping Address</dt>
                  <dd aria-label="Display Shipping Address">{formatAddress(order.shippingAddress)}</dd>
                </div>
              )}
              <div>
                <dt>Country</dt>
                <dd aria-label="Display Country">{order.country}</dd>
              </div>
              <div>
                <dt>Applied Coupon</dt>
                <dd className="font-monospace" aria-label="Display Applied Coupon">
                  {order.appliedCouponCode ?? "None"}
                </dd>
              </div>
            </dl>
          </section>
        </div>
        <div className="col-xl-5">
          <section className="market-panel p-4" aria-labelledby="order-price-heading">
            <h2 className="h5 mb-3" id="order-price-heading">
              Price breakdown
            </h2>
            <dl className="order-breakdown">
              <dt>Base Price</dt>
              <dd aria-label="Display Base Price">${order.basePrice}</dd>
              <dt>Discount Rate</dt>
              <dd aria-label="Display Discount Rate">{percent(order.discountRate)}</dd>
              <dt>Discount Amount</dt>
              <dd aria-label="Display Discount Amount">${order.discountAmount}</dd>
              <dt>Subtotal Price</dt>
              <dd className="fw-bold" aria-label="Display Subtotal Price">
                ${order.subtotalPrice}
              </dd>
              <dt>Tax Rate</dt>
              <dd aria-label="Display Tax Rate">{percent(order.taxRate)}</dd>
              <dt>Tax Amount</dt>
              <dd aria-label="Display Tax Amount">${order.taxAmount}</dd>
            </dl>
            <div className="order-total-row">
              <span>Total Price</span>
              <span className="order-total-value" aria-label="Display Total Price">
                ${order.totalPrice}
              </span>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}

const BREADCRUMBS = [
  { label: "Home", path: "/" },
  { label: "Order History", path: "/order-history" },
  { label: "Order Details" },
];

export default function OrderDetailsPage() {
  return (
    <AccountLayout title="Order Details" breadcrumbs={BREADCRUMBS}>
      <Suspense fallback={<LoadingSpinner message="Loading..." />}>
        <OrderDetailsContent />
      </Suspense>
    </AccountLayout>
  );
}
