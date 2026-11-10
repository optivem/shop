import { type FormEvent, useCallback } from 'react';
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
      <div className="row">
        <div className="col-lg-6 mx-auto">
          <OrderForm
            formData={formData}
            onFormChange={updateFormData}
            onSubmit={(e) => { void handleSubmit(e); }}
            isSubmitting={isSubmitting}
          />
        </div>
      </div>
    </Layout>
  );
}
