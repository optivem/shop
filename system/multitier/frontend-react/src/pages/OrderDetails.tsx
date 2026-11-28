import { useCallback, useState } from 'react';
import { useTranslation } from 'react-i18next';
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
  const { t } = useTranslation('account');
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
      setSuccess(t('orderDetails.cancelled'));
      refresh();
    });
    setIsCancelling(false);
  }, [orderNumber, handleResult, setSuccess, refresh, t]);

  const handleDeliver = useCallback(async () => {
    if (!orderNumber) return;
    setIsDelivering(true);
    handleResult(await orderService.deliverOrder(orderNumber), () => {
      setSuccess(t('orderDetails.delivered'));
      refresh();
    });
    setIsDelivering(false);
  }, [orderNumber, handleResult, setSuccess, refresh, t]);

  // A cancelled or delivered order can't be changed, so its buttons are not rendered at all (drivers count them).
  const canChange = !!order && order.status !== OrderStatus.CANCELLED && order.status !== OrderStatus.DELIVERED;

  return (
    <Layout
      title={t('orderDetails.title')}
      breadcrumbs={[
        { label: t('breadcrumbs.home'), path: '/' },
        { label: t('breadcrumbs.orderHistory'), path: '/order-history' },
        { label: t('breadcrumbs.orderDetails') }
      ]}
    >
      <div className="order-detail">
        <button type="button" className="btn btn-link order-back" onClick={() => navigate('/order-history')}>
          <ArrowLeft size={16} aria-hidden="true" />
          {t('orderDetails.back')}
        </button>
        <DataState
          isLoading={isLoading}
          error={error}
          isEmpty={!order}
          loadingMessage={t('orderDetails.loading')}
          emptyMessage={t('orderDetails.notFound')}
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
                      aria-label={t('orderDetails.cancelOrder')}
                      onClick={() => { void handleCancel(); }}
                      disabled={isCancelling}
                    >
                      {isCancelling ? t('orderDetails.cancelling') : t('orderDetails.cancelOrder')}
                    </button>
                    {isAdmin && (
                      <button
                        type="button"
                        className="btn btn-primary"
                        aria-label={t('orderDetails.deliverOrder')}
                        onClick={() => { void handleDeliver(); }}
                        disabled={isDelivering}
                      >
                        {isDelivering ? t('orderDetails.delivering') : t('orderDetails.deliverOrder')}
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
