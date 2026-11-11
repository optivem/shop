import { useState, type FormEvent, useCallback } from 'react';
import { SubmitButton } from '../../components/SubmitButton';

export interface CouponFormData {
  code: string;
  discountRate: string;
  validFrom: string;
  validTo: string;
  usageLimit: string;
}

export interface CouponFormProps {
  /** Resolves to whether the coupon was saved; the form is reset only on success. */
  onSubmit: (formData: CouponFormData) => Promise<boolean>;
  isSubmitting: boolean;
  generateCouponCode: () => string;
}

export function CouponForm({ onSubmit, isSubmitting, generateCouponCode }: Readonly<CouponFormProps>) {
  const getDefaultFormData = useCallback((): CouponFormData => ({
    code: generateCouponCode(),
    discountRate: '0.2',
    validFrom: '',
    validTo: '',
    usageLimit: ''
  }), [generateCouponCode]);

  const [formData, setFormData] = useState(getDefaultFormData());

  const handleSubmit = useCallback(async (e: FormEvent) => {
    e.preventDefault();
    const saved = await onSubmit(formData);
    if (saved) {
      setFormData(getDefaultFormData());
    }
  }, [onSubmit, formData, getDefaultFormData]);

  return (
    <section className="market-panel coupon-panel coupon-form-panel mb-4" aria-labelledby="coupon-form-heading">
      <div className="coupon-panel-header">
        <div>
          <h2 id="coupon-form-heading" className="coupon-panel-title">Create New Coupon</h2>
          <p className="coupon-panel-subtitle">Customers enter the code at checkout to get the discount.</p>
        </div>
      </div>
      <div className="coupon-panel-body">
        <form className="coupon-form" onSubmit={(e) => { void handleSubmit(e); }}>
          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label htmlFor="code" className="form-label market-label">Coupon Code:</label>
              <input
                type="text"
                className="form-control coupon-code-input"
                id="code"
                aria-label="Coupon Code"
                value={formData.code}
                onChange={(e) => setFormData((prev) => ({ ...prev, code: e.target.value }))}
                placeholder="e.g., SUMMER2026"
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="discountRate" className="form-label market-label">Discount Rate (0-1):</label>
              <input
                type="number"
                className="form-control"
                id="discountRate"
                aria-label="Discount Rate"
                value={formData.discountRate}
                onChange={(e) => setFormData((prev) => ({ ...prev, discountRate: e.target.value }))}
                step="0.01"
                placeholder="e.g., 0.2 for 20% off"
              />
            </div>
          </div>
          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label htmlFor="validFrom" className="form-label market-label">Valid From (Optional):</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validFrom"
                aria-label="Valid From"
                value={formData.validFrom}
                onChange={(e) => setFormData((prev) => ({ ...prev, validFrom: e.target.value }))}
              />
              <small className="form-text text-muted">Leave empty for immediate validity</small>
            </div>
            <div className="col-md-6">
              <label htmlFor="validTo" className="form-label market-label">Valid To (Optional):</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validTo"
                aria-label="Valid To"
                value={formData.validTo}
                onChange={(e) => setFormData((prev) => ({ ...prev, validTo: e.target.value }))}
              />
              <small className="form-text text-muted">Leave empty for no expiration</small>
            </div>
          </div>
          <div className="row g-3">
            <div className="col-md-6">
              <label htmlFor="usageLimit" className="form-label market-label">Usage Limit (Optional):</label>
              <input
                type="number"
                className="form-control"
                id="usageLimit"
                aria-label="Usage Limit"
                value={formData.usageLimit}
                onChange={(e) => setFormData((prev) => ({ ...prev, usageLimit: e.target.value }))}
                placeholder="Leave empty for unlimited"
              />
            </div>
            <div className="col-md-6 d-flex align-items-end">
              <SubmitButton
                isSubmitting={isSubmitting}
                text="Create Coupon"
                loadingText="Creating..."
                ariaLabel="Create Coupon"
                className="btn btn-primary w-100 coupon-submit"
              />
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}
