import type { ReactNode } from 'react';
import { Layout } from '../components';
import { useRoles } from './useRoles';

/** Renders children only for admins; everyone else sees a "Not authorized" page. */
export function RequireAdmin({ children }: Readonly<{ children: ReactNode }>) {
  const { isAdmin } = useRoles();
  if (isAdmin) return <>{children}</>;
  return (
    <Layout>
      <div className="alert alert-danger" role="alert">
        <h1 className="h4">Not authorized</h1>
        <p className="mb-0">You do not have permission to view this page.</p>
      </div>
    </Layout>
  );
}
