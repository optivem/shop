import { useState, type FormEvent, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('account');
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
          <h2 id="coupon-form-heading" className="coupon-panel-title">{t('couponForm.heading')}</h2>
          <p className="coupon-panel-subtitle">{t('couponForm.subtitle')}</p>
        </div>
      </div>
      <div className="coupon-panel-body">
        <form className="coupon-form" onSubmit={(e) => { void handleSubmit(e); }}>
          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label htmlFor="code" className="form-label market-label">{t('couponForm.codeLabel')}</label>
              <input
                type="text"
                className="form-control coupon-code-input"
                id="code"
                aria-label={t('couponForm.codeAria')}
                value={formData.code}
                onChange={(e) => setFormData((prev) => ({ ...prev, code: e.target.value }))}
                placeholder={t('couponForm.codePlaceholder')}
              />
            </div>
            <div className="col-md-6">
              <label htmlFor="discountRate" className="form-label market-label">{t('couponForm.rateLabel')}</label>
              <input
                type="number"
                className="form-control"
                id="discountRate"
                aria-label={t('couponForm.rateAria')}
                value={formData.discountRate}
                onChange={(e) => setFormData((prev) => ({ ...prev, discountRate: e.target.value }))}
                step="0.01"
                placeholder={t('couponForm.ratePlaceholder')}
              />
            </div>
          </div>
          <div className="row g-3 mb-3">
            <div className="col-md-6">
              <label htmlFor="validFrom" className="form-label market-label">{t('couponForm.validFromLabel')}</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validFrom"
                aria-label={t('couponForm.validFromAria')}
                value={formData.validFrom}
                onChange={(e) => setFormData((prev) => ({ ...prev, validFrom: e.target.value }))}
              />
              <small className="form-text text-muted">{t('couponForm.validFromHint')}</small>
            </div>
            <div className="col-md-6">
              <label htmlFor="validTo" className="form-label market-label">{t('couponForm.validToLabel')}</label>
              <input
                type="datetime-local"
                className="form-control"
                id="validTo"
                aria-label={t('couponForm.validToAria')}
                value={formData.validTo}
                onChange={(e) => setFormData((prev) => ({ ...prev, validTo: e.target.value }))}
              />
              <small className="form-text text-muted">{t('couponForm.validToHint')}</small>
            </div>
          </div>
          <div className="row g-3">
            <div className="col-md-6">
              <label htmlFor="usageLimit" className="form-label market-label">{t('couponForm.usageLabel')}</label>
              <input
                type="number"
                className="form-control"
                id="usageLimit"
                aria-label={t('couponForm.usageAria')}
                value={formData.usageLimit}
                onChange={(e) => setFormData((prev) => ({ ...prev, usageLimit: e.target.value }))}
                placeholder={t('couponForm.usagePlaceholder')}
              />
            </div>
            <div className="col-md-6 d-flex align-items-end">
              <SubmitButton
                isSubmitting={isSubmitting}
                text={t('couponForm.create')}
                loadingText={t('couponForm.creating')}
                ariaLabel={t('couponForm.create')}
                className="btn btn-primary w-100 coupon-submit"
              />
            </div>
          </div>
        </form>
      </div>
    </section>
  );
}
