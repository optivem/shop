"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

interface Coupon {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount: number;
}

function formatDateOrFallback(dateStr: string | undefined, fallback: string): string {
  if (!dateStr) return fallback;
  return new Date(dateStr).toLocaleString("en-US", { timeZone: "UTC" });
}

const formatDate = (dateStr?: string) => formatDateOrFallback(dateStr, "Immediate");
const formatValidTo = (dateStr?: string) => formatDateOrFallback(dateStr, "Never");

function formatUsageLimit(value?: number): string {
  if (value === undefined || value === null || value === 2147483647)
    return "Unlimited";
  return value.toString();
}

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
        const data = await response.json();
        setCoupons(data.coupons || []);
      }
    } catch {
      // ignore
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    loadCoupons();
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();

    const nextId = notificationCounter + 1;
    setNotificationCounter(nextId);

    const body: Record<string, unknown> = {
      code,
      discountRate: Number.parseFloat(discountRate),
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
        setTimeout(loadCoupons, 100);
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
          message: data.detail || "Failed to create coupon",
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
    <>
      <nav aria-label="breadcrumb">
        <ol className="breadcrumb">
          <li className="breadcrumb-item">
            <Link href="/">Home</Link>
          </li>
          <li className="breadcrumb-item active" aria-current="page">
            Coupon Management
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

      <div className="card shadow mb-4">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Create New Coupon</h4>
        </div>
        <div className="card-body">
          <form onSubmit={handleSubmit}>
            <div className="row mb-3">
              <div className="col-md-6">
                <label htmlFor="code" className="form-label">
                  Coupon Code:
                </label>
                <input
                  type="text"
                  className="form-control"
                  id="code"
                  aria-label="Coupon Code"
                  placeholder="e.g., SUMMER2026"
                  value={code}
                  onChange={(e) => setCode(e.target.value)}
                />
              </div>
              <div className="col-md-6">
                <label htmlFor="discountRate" className="form-label">
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
            <div className="row mb-3">
              <div className="col-md-6">
                <label htmlFor="validFrom" className="form-label">
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
                <small className="form-text text-muted">
                  Leave empty for immediate validity
                </small>
              </div>
              <div className="col-md-6">
                <label htmlFor="validTo" className="form-label">
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
                <small className="form-text text-muted">
                  Leave empty for no expiration
                </small>
              </div>
            </div>
            <div className="row mb-3">
              <div className="col-md-6">
                <label htmlFor="usageLimit" className="form-label">
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
                <button
                  type="submit"
                  className="btn btn-primary w-100"
                  aria-label="Create Coupon"
                >
                  Create Coupon
                </button>
              </div>
            </div>
          </form>
        </div>
      </div>

      <div className="card shadow">
        <div className="card-header bg-primary text-white d-flex justify-content-between align-items-center">
          <h4 className="mb-0">Existing Coupons</h4>
          <button
            className="btn btn-light btn-sm"
            aria-label="Refresh Coupon List"
            onClick={loadCoupons}
          >
            Refresh
          </button>
        </div>
        <div className="card-body">
          {loading && (
            <div className="text-center py-3">
              <output>
                <div className="spinner-border text-primary">
                  <span className="visually-hidden">Loading...</span>
                </div>
              </output>
            </div>
          )}
          {!loading && coupons.length === 0 && (
            <div className="text-center py-3 text-muted">
              <p>No coupons found</p>
            </div>
          )}
          {!loading && coupons.length > 0 && (
            <div className="table-responsive">
              <table
                className="table table-striped table-hover"
                aria-label="Coupons Table"
              >
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
                  {coupons.map((coupon) => (
                    <tr key={coupon.code}>
                      <td>{coupon.code}</td>
                      <td>{(coupon.discountRate * 100).toFixed(2)}%</td>
                      <td>{formatDate(coupon.validFrom)}</td>
                      <td>{formatValidTo(coupon.validTo)}</td>
                      <td>{formatUsageLimit(coupon.usageLimit)}</td>
                      <td>{coupon.usedCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
