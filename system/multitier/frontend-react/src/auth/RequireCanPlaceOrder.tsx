import type { ReactNode } from 'react';
import { Layout } from '../components';
import { useRoles } from './useRoles';

/** Renders children only for users who may place orders; admins without the customer role see a "Not authorized" page. */
export function RequireCanPlaceOrder({ children }: Readonly<{ children: ReactNode }>) {
  const { canPlaceOrder } = useRoles();
  if (canPlaceOrder) return <>{children}</>;
  return (
    <Layout>
      <div className="alert alert-danger" role="alert">
        <h1 className="h4">Not authorized</h1>
        <p className="mb-0">Administrators cannot place orders.</p>
      </div>
    </Layout>
  );
}
