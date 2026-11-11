import type { ReactNode } from 'react';
import { ShieldAlert } from 'lucide-react';
import { Layout } from '../components';
import { useRoles } from './useRoles';

/** Renders children only for users who may place orders; admins without the customer role see a "Not authorized" page. */
export function RequireCanPlaceOrder({ children }: Readonly<{ children: ReactNode }>) {
  const { canPlaceOrder } = useRoles();
  if (canPlaceOrder) return <>{children}</>;
  return (
    <Layout>
      <div className="alert alert-danger not-authorized" role="alert">
        <ShieldAlert className="not-authorized-icon" size={28} aria-hidden="true" />
        <div>
          <h1 className="not-authorized-title">Not authorized</h1>
          <p className="mb-0">Administrators cannot place orders.</p>
        </div>
      </div>
    </Layout>
  );
}
