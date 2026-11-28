import type { FormEvent } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('checkout');
  return (
    <section className="market-panel quick-order-panel">
      <p className="order-eyebrow order-eyebrow-accent mb-1">{t('quickOrder.form.eyebrow')}</p>
      <h1 className="h2 mb-1">{t('quickOrder.form.heading')}</h1>
      <p className="text-body-secondary mb-4">{t('quickOrder.form.intro')}</p>
      <form onSubmit={onSubmit} className="quick-order-form">
        <div className="row gx-3">
          <div className="col-12">
            <FormInput
              label={t('quickOrder.form.sku')}
              value={formData.sku}
              onChange={(e) => onFormChange({ sku: e.target.value })}
              placeholder={t('quickOrder.form.skuPlaceholder')}
              ariaLabel={t('quickOrder.form.sku')}
            />
          </div>
          <div className="col-sm-6">
            <FormInput
              label={t('quickOrder.form.quantity')}
              value={formData.quantityValue}
              onChange={(e) => onFormChange({
                quantityValue: e.target.value,
                quantity: Number.parseInt(e.target.value) || 0
              })}
              inputMode="numeric"
              placeholder={t('quickOrder.form.quantityPlaceholder')}
              ariaLabel={t('quickOrder.form.quantity')}
            />
          </div>
          <div className="col-sm-6">
            <FormInput
              label={t('quickOrder.form.country')}
              value={formData.country}
              onChange={(e) => onFormChange({ country: e.target.value })}
              placeholder={t('quickOrder.form.countryPlaceholder')}
              ariaLabel={t('quickOrder.form.country')}
            />
          </div>
          <div className="col-12">
            <FormInput
              label={t('quickOrder.form.coupon')}
              value={formData.couponCode}
              onChange={(e) => onFormChange({ couponCode: e.target.value })}
              placeholder={t('quickOrder.form.couponPlaceholder')}
              ariaLabel={t('quickOrder.form.couponLabel')}
            />
          </div>
        </div>
        <div className="d-grid mt-2">
          <SubmitButton
            isSubmitting={isSubmitting}
            text={t('quickOrder.form.placeOrder')}
            loadingText={t('quickOrder.form.placingOrder')}
            ariaLabel={t('quickOrder.form.placeOrder')}
            className="btn btn-primary btn-lg"
          />
        </div>
      </form>
    </section>
  );
}
