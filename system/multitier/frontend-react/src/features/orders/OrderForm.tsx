import type { FormEvent } from 'react';
import { FormInput, SubmitButton } from '../../components';
import type { OrderFormData } from '../../types/form.types';

export interface OrderFormProps {
  formData: OrderFormData;
  onFormChange: (updates: Partial<OrderFormData>) => void;
  onSubmit: (e: FormEvent) => void;
  isSubmitting: boolean;
}

/**
 * Order placement form component
 * Collects SKU, quantity, country and optional coupon code
 */
export function OrderForm({ formData, onFormChange, onSubmit, isSubmitting }: Readonly<OrderFormProps>) {
  return (
    <section className="market-panel quick-order-panel">
      <p className="order-eyebrow order-eyebrow-accent mb-1">Quick order</p>
      <h1 className="h2 mb-1">Place Your Order</h1>
      <p className="text-body-secondary mb-4">Know the SKU? Order it directly without browsing.</p>
      <form onSubmit={onSubmit} className="quick-order-form">
        <div className="row gx-3">
          <div className="col-12">
            <FormInput
              label="SKU"
              value={formData.sku}
              onChange={(e) => onFormChange({ sku: e.target.value })}
              placeholder="Enter product SKU"
              ariaLabel="SKU"
            />
          </div>
          <div className="col-sm-6">
            <FormInput
              label="Quantity"
              value={formData.quantityValue}
              onChange={(e) => onFormChange({
                quantityValue: e.target.value,
                quantity: Number.parseInt(e.target.value) || 0
              })}
              inputMode="numeric"
              placeholder="Enter quantity"
              ariaLabel="Quantity"
            />
          </div>
          <div className="col-sm-6">
            <FormInput
              label="Country"
              value={formData.country}
              onChange={(e) => onFormChange({ country: e.target.value })}
              placeholder="Enter country code (e.g. US)"
              ariaLabel="Country"
            />
          </div>
          <div className="col-12">
            <FormInput
              label="Coupon Code (optional)"
              value={formData.couponCode}
              onChange={(e) => onFormChange({ couponCode: e.target.value })}
              placeholder="Enter coupon code"
              ariaLabel="Coupon Code"
            />
          </div>
        </div>
        <div className="d-grid mt-2">
          <SubmitButton
            isSubmitting={isSubmitting}
            text="Place Order"
            loadingText="Placing Order..."
            ariaLabel="Place Order"
            className="btn btn-primary btn-lg"
          />
        </div>
      </form>
    </section>
  );
}
