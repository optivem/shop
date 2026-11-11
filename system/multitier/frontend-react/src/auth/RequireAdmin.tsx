import type { ReactNode } from 'react';
import { ShieldAlert } from 'lucide-react';
import { Layout } from '../components';
import { useRoles } from './useRoles';

/** Renders children only for admins; everyone else sees a "Not authorized" page. */
export function RequireAdmin({ children }: Readonly<{ children: ReactNode }>) {
  const { isAdmin } = useRoles();
  if (isAdmin) return <>{children}</>;
  return (
    <Layout>
      <div className="alert alert-danger not-authorized" role="alert">
        <ShieldAlert className="not-authorized-icon" size={28} aria-hidden="true" />
        <div>
          <h1 className="not-authorized-title">Not authorized</h1>
          <p className="mb-0">You do not have permission to view this page.</p>
        </div>
      </div>
    </Layout>
  );
}
