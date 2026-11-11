"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { AccountLayout, LoadingSpinner } from "@/components/StoreLayout";
import { formatFieldErrors, parseJson, placeOrderResponseSchema, readErrorData } from "@/lib/api-types";
import { viewProfile } from "@/lib/storefront/api-client";

const DEFAULT_COUNTRY = "US";
const BREADCRUMBS = [{ label: "Home", path: "/" }, { label: "Quick order" }];

function buildOrderBody(sku: string, quantity: string, country: string, couponCode: string): Record<string, unknown> {
  const body: Record<string, unknown> = {};
  if (sku !== "") body.sku = sku;
  if (quantity !== "") {
    const trimmed = quantity.trim();
    if (trimmed === "") {
      body.quantity = quantity;
    } else {
      const num = Number(trimmed);
      body.quantity = Number.isNaN(num) ? quantity : num;
    }
  }
  if (country !== "") body.country = country;
  if (couponCode !== "") body.couponCode = couponCode;
  return body;
}

/**
 * `/new-order`: Quick order, with its Country prefilled from the customer's saved profile (UC-001 1a, UC-010 BR-5).
 * The form only appears once the profile is read, so a prefill can never overwrite what the customer types.
 */
export default function NewOrderPage() {
  const [initialCountry, setInitialCountry] = useState<string | null>(null);

  useEffect(() => {
    let ignore = false;
    void viewProfile().then((result) => {
      if (ignore) return;
      const saved = result.success ? (result.data.country?.trim() ?? "") : "";
      setInitialCountry(saved === "" ? DEFAULT_COUNTRY : saved);
    });
    return () => {
      ignore = true;
    };
  }, []);

  return (
    <AccountLayout title="Quick order" breadcrumbs={BREADCRUMBS}>
      {initialCountry === null ? (
        <div className="market-panel">
          <LoadingSpinner message="Loading..." />
        </div>
      ) : (
        <NewOrderForm initialCountry={initialCountry} />
      )}
    </AccountLayout>
  );
}

function NewOrderForm({ initialCountry }: Readonly<{ initialCountry: string }>) {
  const [sku, setSku] = useState("");
  const [quantity, setQuantity] = useState("");
  const [country, setCountry] = useState(initialCountry);
  const [couponCode, setCouponCode] = useState("");
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
      const body = buildOrderBody(sku, quantity, country, couponCode);

      const response = await fetch("/api/orders", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        const data = await parseJson(response, placeOrderResponseSchema);
        setNotification({
          type: "success",
          message: `Success! Order has been created with Order Number ${data.orderNumber}`,
          fieldErrors: [],
          id: nextId,
        });
      } else {
        const errorData = await readErrorData(response);
        const fieldErrors = formatFieldErrors(errorData);
        setNotification({
          type: "error",
          message: errorData.detail ?? "An error occurred",
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

  const field = (id: string, label: string, ariaLabel: string, value: string, onChange: (value: string) => void, placeholder: string, extra?: { inputMode?: "numeric" }) => (
    <div className="mb-3">
      <label htmlFor={id} className="form-label market-label">
        {label}:
      </label>
      <input
        type="text"
        className="form-control"
        id={id}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={ariaLabel}
        {...extra}
      />
    </div>
  );

  return (
    <>
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

      <div className="row g-4 align-items-start">
        <div className="col-xl-8">
          <section className="market-panel quick-order-panel">
            <p className="order-eyebrow order-eyebrow-accent mb-1">Quick order</p>
            <h1 className="h2 mb-1">Place Your Order</h1>
            <p className="text-body-secondary mb-4">Know the SKU? Order it directly without browsing.</p>
            <form
              className="quick-order-form"
              onSubmit={(e) => {
                void handleSubmit(e);
              }}
            >
              <div className="row gx-3">
                <div className="col-12">{field("sku", "SKU", "SKU", sku, setSku, "Enter product SKU")}</div>
                <div className="col-sm-6">
                  {field("quantity", "Quantity", "Quantity", quantity, setQuantity, "Enter quantity", { inputMode: "numeric" })}
                </div>
                <div className="col-sm-6">{field("country", "Country", "Country", country, setCountry, "Enter country code (e.g. US)")}</div>
                <div className="col-12">
                  {field("couponCode", "Coupon Code (optional)", "Coupon Code", couponCode, setCouponCode, "Enter coupon code")}
                </div>
              </div>
              <div className="d-grid mt-2">
                <button type="submit" className="btn btn-primary btn-lg" disabled={submitting} aria-label="Place Order">
                  {submitting && <span className="spinner-border spinner-border-sm me-2" aria-hidden="true" />}
                  {submitting ? "Placing Order..." : "Place Order"}
                </button>
              </div>
            </form>
          </section>
        </div>
        <aside className="col-xl-4 d-flex flex-column gap-3" aria-label="Quick order help">
          <div className="market-panel p-4">
            <h2 className="h6 fw-bold mb-2">Tips</h2>
            <ul className="quick-order-tips mb-0">
              <li>The SKU is shown on every product page.</li>
              <li>Country is a two-letter code, such as US, DE or GB.</li>
              <li>Tax is calculated from the country.</li>
            </ul>
          </div>
          <div className="quick-order-promo">
            <p className="fw-bold mb-1">Ordering several items?</p>
            <Link href="/" className="quick-order-promo-link">
              Browse the catalog and use the cart
              <Icon name="arrow-right" />
            </Link>
          </div>
        </aside>
      </div>
    </>
  );
}
