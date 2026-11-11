"use client";

import { useState, useEffect } from "react";
import { Icon } from "@/components/Icon";
import { AccountLayout, LoadingSpinner } from "@/components/StoreLayout";
import {
  couponsResponseSchema,
  formatFieldErrors,
  parseJson,
  readErrorData,
  type Coupon,
} from "@/lib/api-types";

function formatDateOrFallback(dateStr: string | null | undefined, fallback: string): string {
  if (!dateStr) return fallback;
  return new Date(dateStr).toLocaleString("en-US", { timeZone: "UTC" });
}

const formatDate = (dateStr?: string | null) => formatDateOrFallback(dateStr, "Immediate");
const formatValidTo = (dateStr?: string | null) => formatDateOrFallback(dateStr, "Never");

function formatUsageLimit(value?: number | null): string {
  if (value === undefined || value === null || value === 2147483647)
    return "Unlimited";
  return value.toString();
}

const BREADCRUMBS = [{ label: "Home", path: "/" }, { label: "Coupon Management" }];

export default function AdminCouponsPage() {
  const [coupons, setCoupons] = useState<Coupon[]>([]);
  const [loading, setLoading] = useState(true);
  const [code, setCode] = useState("");
  const [discountRate, setDiscountRate] = useState("");
  const [validFrom, setValidFrom] = useState("");
  const [validTo, setValidTo] = useState("");
  const [usageLimit, setUsageLimit] = useState("");
  const [notification, setNotification] = useState<{
    type: "success" | "error";
    message: string;
    fieldErrors: string[];
    id: number;
  } | null>(null);
  const [notificationCounter, setNotificationCounter] = useState(0);

  async function loadCoupons() {
    setLoading(true);
    try {
      const response = await fetch("/api/coupons");
      if (response.ok) {
        const data = await parseJson(response, couponsResponseSchema);
        setCoupons(data.coupons);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    void loadCoupons();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    const nextId = notificationCounter + 1;
    setNotificationCounter(nextId);

    const body: Record<string, unknown> = {
      code,
      // Rates travel as decimal strings; a blank input still sends null, as before.
      discountRate: discountRate.trim() === "" ? null : discountRate.trim(),
    };

    if (validFrom.trim()) {
      body.validFrom = new Date(validFrom + "Z").toISOString();
    }
    if (validTo.trim()) {
      body.validTo = new Date(validTo + "Z").toISOString();
    }
    if (usageLimit.trim()) {
      body.usageLimit = Number.parseInt(usageLimit, 10);
    }

    try {
      const response = await fetch("/api/coupons", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (response.ok) {
        setNotification({
          type: "success",
          message: `Coupon '${code}' created successfully!`,
          fieldErrors: [],
          id: nextId,
        });
        setCode("");
        setDiscountRate("");
        setValidFrom("");
        setValidTo("");
        setUsageLimit("");
        setTimeout(() => {
          void loadCoupons();
        }, 100);
      } else {
        const data = await readErrorData(response);
        const fieldErrors = formatFieldErrors(data);
        setNotification({
          type: "error",
          message: data.detail ?? "Failed to create coupon",
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
    }
  }

  return (
    <AccountLayout title="Coupon Management" breadcrumbs={BREADCRUMBS}>
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

      <h1 className="admin-coupons-title">Coupons</h1>
      <section className="market-panel coupon-panel coupon-form-panel mb-4" aria-labelledby="coupon-form-heading">
        <div className="coupon-panel-header">
          <div>
            <h2 id="coupon-form-heading" className="coupon-panel-title">
              Create New Coupon
            </h2>
            <p className="coupon-panel-subtitle">Customers enter the code at checkout to get the discount.</p>
          </div>
        </div>
        <div className="coupon-panel-body">
          <form
            className="coupon-form"
            onSubmit={(e) => {
              void handleSubmit(e);
            }}
          >
            <div className="row g-3 mb-3">
              <div className="col-md-6">
                <label htmlFor="code" className="form-label market-label">
                  Coupon Code:
                </label>
                <input
                  type="text"
                  className="form-control coupon-code-input"
                  id="code"
                  aria-label="Coupon Code"
                  placeholder="e.g., SUMMER2026"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="discountRate" className="form-label market-label">
                  Discount Rate (0-1):
                </label>
                <input
                  type="number"
                  className="form-control"
                  id="discountRate"
                  aria-label="Discount Rate"
                  step="0.01"
                  placeholder="e.g., 0.2 for 20% off"
                  value={discountRate}
                  onChange={(e) => setDiscountRate(e.target.value)}
                />
              </div>
            </div>
            <div className="row g-3 mb-3">
              <div className="col-md-6">
                <label htmlFor="validFrom" className="form-label market-label">
                  Valid From (Optional):
                </label>
                <input
                  type="datetime-local"
                  className="form-control"
                  id="validFrom"
                  aria-label="Valid From"
                  value={validFrom}
                  onChange={(e) => setValidFrom(e.target.value)}
                />
                <small className="form-text text-muted">Leave empty for immediate validity</small>
              </div>
              <div className="col-md-6">
                <label htmlFor="validTo" className="form-label market-label">
                  Valid To (Optional):
                </label>
                <input
                  type="datetime-local"
                  className="form-control"
                  id="validTo"
                  aria-label="Valid To"
                  value={validTo}
                  onChange={(e) => setValidTo(e.target.value)}
                />
                <small className="form-text text-muted">Leave empty for no expiration</small>
              </div>
            </div>
            <div className="row g-3">
              <div className="col-md-6">
                <label htmlFor="usageLimit" className="form-label market-label">
                  Usage Limit (Optional):
                </label>
                <input
                  type="number"
                  className="form-control"
                  id="usageLimit"
                  aria-label="Usage Limit"
                  placeholder="Leave empty for unlimited"
                  value={usageLimit}
                  onChange={(e) => setUsageLimit(e.target.value)}
                />
              </div>
              <div className="col-md-6 d-flex align-items-end">
                <button type="submit" className="btn btn-primary w-100 coupon-submit" aria-label="Create Coupon">
                  Create Coupon
                </button>
              </div>
            </div>
          </form>
        </div>
      </section>

      <section className="market-panel coupon-panel" aria-labelledby="coupon-table-heading">
        <div className="coupon-panel-header">
          <div>
            <h2 id="coupon-table-heading" className="coupon-panel-title">
              Existing Coupons
            </h2>
            {!loading && (
              <p className="coupon-panel-subtitle">
                {coupons.length} {coupons.length === 1 ? "coupon" : "coupons"}
              </p>
            )}
          </div>
          <button
            type="button"
            className="btn btn-outline-secondary coupon-refresh"
            aria-label="Refresh Coupon List"
            disabled={loading}
            onClick={() => {
              void loadCoupons();
            }}
          >
            <Icon name="refresh-cw" />
            Refresh
          </button>
        </div>
        {loading && <LoadingSpinner message="Loading coupons..." />}
        {!loading && (
          <div className="table-responsive coupon-table-scroll">
            <table className="table table-hover align-middle mb-0 coupon-table" aria-label="Coupons Table">
              <thead>
                <tr>
                  <th>Code</th>
                  <th>Discount Rate</th>
                  <th>Valid From</th>
                  <th>Valid To</th>
                  <th>Usage Limit</th>
                  <th>Used Count</th>
                </tr>
              </thead>
              <tbody>
                {coupons.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="text-center py-5 text-body-secondary fw-semibold">
                      No coupons found
                    </td>
                  </tr>
                ) : (
                  coupons.map((coupon) => (
                    <tr key={coupon.code}>
                      <td>{coupon.code}</td>
                      <td>{(Number(coupon.discountRate) * 100).toFixed(2)}%</td>
                      <td>{formatDate(coupon.validFrom)}</td>
                      <td>{formatValidTo(coupon.validTo)}</td>
                      <td>{formatUsageLimit(coupon.usageLimit)}</td>
                      <td>{coupon.usedCount}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </AccountLayout>
  );
}
