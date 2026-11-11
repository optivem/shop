"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { LoadingSpinner, StoreLayout } from "@/components/StoreLayout";
import { useCatalog } from "@/components/StorefrontProviders";
import { loadConfirmation } from "@/lib/storefront/confirmation-store";
import type { CheckoutResult } from "@/lib/storefront/place-checkout-orders";

const orderDetailsPath = (orderNumber: string) => `/order-details?orderNumber=${encodeURIComponent(orderNumber)}`;

/** `/checkout/confirmation`: the orders just placed (and, after a partial failure, the rejected lines). */
export default function OrderConfirmationPage() {
  // The result is in sessionStorage, which only the browser has: read once the page is in the browser.
  const [result, setResult] = useState<CheckoutResult | undefined | null>(null);

  useEffect(() => {
    setResult(loadConfirmation());
  }, []);

  const { products } = useCatalog();
  const titleOf = (sku: string) => products.find((product) => product.sku === sku)?.title;
  const placedCount = result?.placed.length ?? 0;
  const rejectedCount = result?.rejected.length ?? 0;

  const renderResult = (loaded: CheckoutResult | undefined) =>
    loaded && placedCount > 0 ? (
      <>
        <section className="market-panel order-confirmation-hero">
          <div className="order-confirmation-icon" aria-hidden="true">
            <Icon name="circle-check" size={32} />
          </div>
          <h1 className="h2 mb-1">Thank you for your order!</h1>
          <p className="text-body-secondary mb-0">
            {placedCount === 1 ? "Your order has been placed:" : `Your ${placedCount} orders have been placed:`}
          </p>
        </section>
        <section className="market-panel p-4" aria-labelledby="confirmed-orders-heading">
          <h2 className="h6 fw-bold mb-3" id="confirmed-orders-heading">
            Confirmed orders
          </h2>
          <ul className="confirmed-orders" aria-label="Confirmed Orders">
            {loaded.placed.map((line) => (
              <li key={line.orderNumber} className="confirmed-order">
                <div className="min-w-0">
                  <Link
                    href={orderDetailsPath(line.orderNumber)}
                    className="order-number-link"
                    aria-label={`Confirmed Order ${line.orderNumber}`}
                  >
                    {line.orderNumber}
                  </Link>
                  <p className="confirmed-order-meta mb-0">
                    {titleOf(line.sku) ?? line.sku} × {line.quantity}
                    {titleOf(line.sku) && <span className="font-monospace"> · {line.sku}</span>}
                  </p>
                </div>
                <span className="order-status status-PLACED">PLACED</span>
              </li>
            ))}
          </ul>
        </section>
      </>
    ) : (
      <section className="market-empty text-center py-5 px-3">
        <Icon name="package-search" size={40} className="mb-3 text-body-secondary" />
        <h1 className="h3">No recent order</h1>
        <p className="mb-0">Orders you place at checkout show up here right after.</p>
      </section>
    );

  return (
    <StoreLayout title="Order confirmation" breadcrumbs={[{ label: "Home", path: "/" }, { label: "Order confirmation" }]}>
      <div className="order-confirmation">
        {result === null ? <LoadingSpinner message="Loading..." /> : renderResult(result)}
        {result && rejectedCount > 0 && (
          <div className="alert alert-warning order-rejected mb-0" aria-label="Rejected Lines">
            <h2 className="h6 fw-bold d-flex align-items-center gap-2 mb-2">
              <Icon name="triangle-alert" size={18} />
              {rejectedCount === 1 ? "1 item could not be ordered" : `${rejectedCount} items could not be ordered`}
            </h2>
            <p className="mb-2">These items were not ordered and are still in your cart:</p>
            <ul className="mb-0">
              {result.rejected.map((line) => (
                <li key={line.sku} aria-label={`Rejected Line ${line.sku}`}>
                  <strong>{line.sku}</strong>: <span aria-label={`Rejection Reason ${line.sku}`}>{line.reason}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="order-confirmation-actions">
          <Link href="/order-history" className="btn btn-primary btn-lg">
            View my orders
          </Link>
          <Link href="/" className="btn btn-outline-secondary btn-lg">
            Continue shopping
          </Link>
        </div>
      </div>
    </StoreLayout>
  );
}
