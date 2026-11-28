import { useTranslation } from 'react-i18next';
import { useRoles } from '../auth/useRoles';
import { Layout } from '../components';
import { OrderHistoryTable } from '../features/orders';
import { useOrders } from '../hooks';

/**
 * Order History page: My orders for a customer, Admin › All orders for an admin (UC-002, UC-011).
 * Filters by order number; an admin can also filter by the customer who placed the order.
 */
export function OrderHistory() {
  const { t } = useTranslation('account');
  const { isAdmin } = useRoles();
  const { orders, filter, setFilter, customer, setCustomer, customers, isLoading, error, refresh } = useOrders('', {
    loadCustomers: isAdmin,
  });

  return (
    <Layout
      title={isAdmin ? t('breadcrumbs.allOrders') : t('breadcrumbs.myOrders')}
      breadcrumbs={[
        { label: t('breadcrumbs.home'), path: '/' },
        ...(isAdmin ? [{ label: t('breadcrumbs.admin') }, { label: t('breadcrumbs.allOrders') }] : [{ label: t('breadcrumbs.myOrders') }]),
      ]}
    >
      <OrderHistoryTable
        orders={orders}
        filter={filter}
        onFilterChange={setFilter}
        customerFilter={isAdmin ? { value: customer, options: customers, onChange: setCustomer } : undefined}
        isLoading={isLoading}
        error={error}
        onRefresh={refresh}
      />
    </Layout>
  );
}
