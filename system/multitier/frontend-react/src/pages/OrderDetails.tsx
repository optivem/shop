import { useCallback, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Layout, DataState } from '../components';
import { OrderDetailView } from '../features/orders';
import { useOrderDetails } from '../hooks';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { orderService } from '../services/order-service';
import { useRoles } from '../auth/useRoles';
import { OrderStatus } from '../types/api.types';

export function OrderDetails() {
  const { orderNumber } = useParams<{ orderNumber: string }>();
  const navigate = useNavigate();
  const { isAdmin } = useRoles();
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

  // A cancelled or delivered order can't be changed, so its buttons are not rendered at all (drivers count them).
  const canChange = !!order && order.status !== OrderStatus.CANCELLED && order.status !== OrderStatus.DELIVERED;

  return (
    <Layout
      title="Order Details"
      breadcrumbs={[
        { label: 'Home', path: '/' },
        { label: 'Order History', path: '/order-history' },
        { label: 'Order Details' }
      ]}
    >
      <div className="order-detail">
        <button type="button" className="btn btn-link order-back" onClick={() => navigate('/order-history')}>
          <ArrowLeft size={16} aria-hidden="true" />
          Back to Order History
        </button>
        <DataState
          isLoading={isLoading}
          error={error}
          isEmpty={!order}
          loadingMessage="Loading order details..."
          emptyMessage="Order not found"
        >
          {order && (
            <OrderDetailView
              order={order}
              actions={
                canChange && (
                  <>
                    <button
                      type="button"
                      className="btn btn-outline-danger"
                      aria-label="Cancel Order"
                      onClick={() => { void handleCancel(); }}
                      disabled={isCancelling}
                    >
                      {isCancelling ? 'Cancelling...' : 'Cancel Order'}
                    </button>
                    {isAdmin && (
                      <button
                        type="button"
                        className="btn btn-primary"
                        aria-label="Deliver Order"
                        onClick={() => { void handleDeliver(); }}
                        disabled={isDelivering}
                      >
                        {isDelivering ? 'Delivering...' : 'Deliver Order'}
                      </button>
                    )}
                  </>
                )
              }
            />
          )}
        </DataState>
      </div>
    </Layout>
  );
}
