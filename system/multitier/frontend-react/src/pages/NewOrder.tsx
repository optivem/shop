import { type FormEvent, useCallback } from 'react';
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
      setSuccess(`Success! Order has been created with Order Number ${data.orderNumber}`);
    });
  }, [submitOrder, handleResult, setSuccess]);

  return (
    <Layout title="Quick order" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Quick order' }]}>
      <div className="row g-4 align-items-start">
        <div className="col-xl-8">
          <OrderForm
            formData={formData}
            onFormChange={updateFormData}
            onSubmit={(e) => { void handleSubmit(e); }}
            isSubmitting={isSubmitting}
          />
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
            <Link to="/" className="quick-order-promo-link">
              Browse the catalog and use the cart
              <ArrowRight size={16} aria-hidden="true" />
            </Link>
          </div>
        </aside>
      </div>
    </Layout>
  );
}
