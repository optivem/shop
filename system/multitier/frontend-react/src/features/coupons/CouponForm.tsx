import { useState, FormEvent, useCallback } from 'react';
import { SubmitButton } from '../../components/SubmitButton';

export interface CouponFormData {
  code: string;
  discountRate: number;
  validFrom: string;
  validTo: string;
  usageLimit: string;
}

export interface CouponFormProps {
  onSubmit: (formData: CouponFormData) => Promise<void>;
  isSubmitting: boolean;
  generateCouponCode: () => string;
}

export function CouponForm({ onSubmit, isSubmitting, generateCouponCode }: Readonly<CouponFormProps>) {
  const getDefaultFormData = useCallback(() => ({
    code: generateCouponCode(),
    discountRate: 0.2,
    validFrom: '',
    validTo: '',
    usageLimit: ''
  }), [generateCouponCode]);

  const [formData, setFormData] = useState(getDefaultFormData());

  const handleSubmit = useCallback(async (e: FormEvent) => {
    e.preventDefault();
    await onSubmit(formData);
    setFormData(getDefaultFormData());
  }, [onSubmit, formData, getDefaultFormData]);

  return (
    <div className="card shadow mb-4">
      <div className="card-header bg-primary text-white">
        <h4 className="mb-0">Create New Coupon</h4>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit}>
          <div className="row mb-3">
            <div className="col-md-6">
              <label htmlFor="code" className="form-label">Coupon Code:</label>
              <input
                type="text"
                className="form-control"
                id="code"
                aria-label="Coupon Code"
                value={formData.code}
                onChange={(e) => setFormData({ ...formData, code: e.target.value })}
                placeholder="e.g., SUMMER2026"
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="discountRate" className="form-label">Discount Rate (0-1):</label>
              <input
                type="number"
                className="form-control"
                id="discountRate"
                aria-label="Discount Rate"
                value={formData.discountRate}
                onChange={(e) => setFormData({ ...formData, discountRate: Number.parseFloat(e.target.value) })}
                step="0.01"
                placeholder="e.g., 0.2 for 20% off"
              />
            </div>
          </div>
          <div className="row mb-3">
            <div className="col-md-6">
              <label htmlFor="validFrom" className="form-label">Valid From (Optional):</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validFrom"
                aria-label="Valid From"
                value={formData.validFrom}
                onChange={(e) => setFormData({ ...formData, validFrom: e.target.value })}
              />
              <small className="form-text text-muted">Leave empty for immediate validity</small>
            </div>
            <div className="col-md-6">
              <label htmlFor="validTo" className="form-label">Valid To (Optional):</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validTo"
                aria-label="Valid To"
                value={formData.validTo}
                onChange={(e) => setFormData({ ...formData, validTo: e.target.value })}
              />
              <small className="form-text text-muted">Leave empty for no expiration</small>
            </div>
          </div>
          <div className="row mb-3">
            <div className="col-md-6">
              <label htmlFor="usageLimit" className="form-label">Usage Limit (Optional):</label>
              <input
                type="number"
                className="form-control"
                id="usageLimit"
                aria-label="Usage Limit"
                value={formData.usageLimit}
                onChange={(e) => setFormData({ ...formData, usageLimit: e.target.value })}
                placeholder="Leave empty for unlimited"
              />
            </div>
            <div className="col-md-6 d-flex align-items-end">
              <SubmitButton
                isSubmitting={isSubmitting}
                text="Create Coupon"
                loadingText="Creating..."
                ariaLabel="Create Coupon"
                className="btn btn-primary w-100"
              />
            </div>
          </div>
        </form>
      </div>
    </div>
  );
}
