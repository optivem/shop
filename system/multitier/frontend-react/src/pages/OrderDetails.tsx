import { useCallback, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Layout, DataState } from '../components';
import { OrderDetailView } from '../features/orders';
import { useOrderDetails } from '../hooks';
import { useNotificationContext } from '../contexts/NotificationContext';
import { orderService } from '../services/order-service';

export function OrderDetails() {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const navigate = useNavigate();
  const { order, isLoading, error, refresh } = useOrderDetails(orderNumber);
  const { handleResult, setSuccess } = useNotificationContext();
  const [isCancelling, setIsCancelling] = useState(false);
  const [isDelivering, setIsDelivering] = useState(false);

  const handleCancel = useCallback(async () => {
    if (!orderNumber) return;
    setIsCancelling(true);
    handleResult(await orderService.cancelOrder(orderNumber), () => {
      setSuccess('Order has been cancelled successfully');
      refresh();
    });
    setIsCancelling(false);
  }, [orderNumber, handleResult, setSuccess, refresh]);

  const handleDeliver = useCallback(async () => {
    if (!orderNumber) return;
    setIsDelivering(true);
    handleResult(await orderService.deliverOrder(orderNumber), () => {
      setSuccess('Order has been delivered successfully');
      refresh();
    });
    setIsDelivering(false);
  }, [orderNumber, handleResult, setSuccess, refresh]);

  return (
    <Layout
      title="Order Details"
      breadcrumbs={[
        { label: 'Home', path: '/' },
        { label: 'Order History', path: '/order-history' },
        { label: 'Order Details' }
      ]}
    >
      <div className="card shadow">
        <div className="card-header bg-primary text-white">
          <h4 className="mb-0">Order Details</h4>
        </div>
        <div className="card-body">
          <DataState
            isLoading={isLoading}
            error={error}
            isEmpty={!order}
            loadingMessage="Loading order details..."
            emptyMessage="Order not found"
          >
            {order && (
              <>
                <OrderDetailView order={order} />
                <div className="mt-4 d-flex gap-2">
                  {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                    <button
                      className="btn btn-danger"
                      aria-label="Cancel Order"
                      onClick={handleCancel}
                      disabled={isCancelling}
                    >
                      {isCancelling ? 'Cancelling...' : 'Cancel Order'}
                    </button>
                  )}
                  {order.status !== 'CANCELLED' && order.status !== 'DELIVERED' && (
                    <button
                      className="btn btn-warning"
                      aria-label="Deliver Order"
                      onClick={handleDeliver}
                      disabled={isDelivering}
                    >
                      {isDelivering ? 'Delivering...' : 'Deliver Order'}
                    </button>
                  )}
                  <button
                    className="btn btn-secondary"
                    onClick={() => navigate('/order-history')}
                  >
                    Back to Order History
                  </button>
                </div>
              </>
            )}
          </DataState>
        </div>
      </div>
    </Layout>
  );
}
