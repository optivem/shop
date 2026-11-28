import { type FormEvent, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import { Layout } from '../components';
import { OrderForm } from '../features/orders';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { useOrderForm } from '../hooks';

/**
 * New Order (Quick order) page component for placing orders by SKU
 * Provides a form interface for customers to submit orders with SKU and quantity
 * @param initialCountry - The country the form starts with; defaults to US
 */
export function NewOrder({ initialCountry }: Readonly<{ initialCountry?: string }> = {}) {
  const { t } = useTranslation('checkout');
  const { setSuccess, handleResult } = useNotificationContext();
  const {
    formData,
    updateFormData,
    isSubmitting,
    submitOrder
  } = useOrderForm(initialCountry);

  const handleSubmit = useCallback(async (e: FormEvent) => {
    e.preventDefault();
    handleResult(await submitOrder(), (data) => {
      setSuccess(t('quickOrder.success', { orderNumber: data.orderNumber }));
    });
  }, [submitOrder, handleResult, setSuccess, t]);

  return (
    <Layout title={t('nav.quickOrder')} breadcrumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.quickOrder') }]}>
      <div className="row g-4 align-items-start">
        <div className="col-xl-8">
          <OrderForm
            formData={formData}
            onFormChange={updateFormData}
            onSubmit={(e) => { void handleSubmit(e); }}
            isSubmitting={isSubmitting}
          />
        </div>
        <aside className="col-xl-4 d-flex flex-column gap-3" aria-label={t('quickOrder.helpLabel')}>
          <div className="market-panel p-4">
            <h2 className="h6 fw-bold mb-2">{t('quickOrder.tips')}</h2>
            <ul className="quick-order-tips mb-0">
              <li>{t('quickOrder.tipSku')}</li>
              <li>{t('quickOrder.tipCountry')}</li>
              <li>{t('quickOrder.tipTax')}</li>
            </ul>
          </div>
          <div className="quick-order-promo">
            <p className="fw-bold mb-1">{t('quickOrder.severalItems')}</p>
            <Link to="/" className="quick-order-promo-link">
              {t('quickOrder.browseCatalog')}
              <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </div>
        </aside>
      </div>
    </Layout>
  );
}
