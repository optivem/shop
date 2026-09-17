import { FormEvent, useCallback } from 'react';
import { Layout } from '../components';
import { OrderForm } from '../features/orders';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { useOrderForm } from '../hooks';

/**
 * New Order page component for placing orders
 * Provides a form interface for customers to submit orders with SKU and quantity
 */
export function NewOrder() {
  const { setSuccess, handleResult } = useNotificationContext();
  const {
    formData,
    updateFormData,
    isSubmitting,
    submitOrder
  } = useOrderForm();

  const handleSubmit = useCallback(async (e: FormEvent) => {
    e.preventDefault();
    handleResult(await submitOrder(), (data) => {
      setSuccess(`Success! Order has been created with Order Number ${data.orderNumber}`);
    });
  }, [submitOrder, handleResult, setSuccess]);

  return (
    <Layout title="New Order" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'New Order' }]}>
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
