import { useRoles } from '../auth/useRoles';
import { Layout } from '../components';
import { OrderHistoryTable } from '../features/orders';
import { useOrders } from '../hooks';

/**
 * Order History page: My orders for a customer, Admin › All orders for an admin (UC-002, UC-011).
 * Filters by order number; an admin can also filter by the customer who placed the order.
 */
export function OrderHistory() {
  const { isAdmin } = useRoles();
  const { orders, filter, setFilter, customer, setCustomer, customers, isLoading, error, refresh } = useOrders('', {
    collectCustomers: isAdmin,
  });

  return (
    <Layout
      title={isAdmin ? 'All orders' : 'My orders'}
      breadcrumbs={[{ label: 'Home', path: '/' }, ...(isAdmin ? [{ label: 'Admin' }, { label: 'All orders' }] : [{ label: 'My orders' }])]}
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
