"use client";

import { useState, useEffect } from "react";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";
import Link from "next/link";

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

      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Order Details</h4>
        </div>
        <div className="card-body">
          {loading ? (
            <div className="text-center py-5">
              <div className="spinner-border text-primary" role="status">
                <span className="visually-hidden">Loading...</span>
              </div>
              <p className="mt-3">Loading order details...</p>
            </div>
          ) : error ? (
            <div
              className="alert alert-danger d-flex justify-content-between align-items-center"
              role="alert"
            >
              <div>
                <strong>Error:</strong> {error}
              </div>
            </div>
          ) : !order ? (
            <div className="text-center py-5 text-muted">
              <p>Order not found</p>
            </div>
          ) : (
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
                  <strong>Total Price:</strong>
                  <p
                    className="fs-5 fw-bold"
                    aria-label="Display Total Price"
                  >
                    ${order.totalPrice.toFixed(2)}
                  </p>
                </div>
              </div>
              <div className="mt-4">
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
          <div className="spinner-border text-primary" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
          <p className="mt-3">Loading...</p>
        </div>
      }
    >
      <OrderDetailsContent />
    </Suspense>
  );
}
