import type { ReactNode } from 'react';
import { StoreLayout, type BreadcrumbItem } from './StoreLayout';
import { AccountNav } from './AccountNav';

interface LayoutProps {
  children: ReactNode;
  title?: string;
  breadcrumbs?: BreadcrumbItem[];
}

/**
 * Layout of the signed-in area (Quick order, My orders, order details, admin pages, My account): the store
 * chrome plus the account or admin side navigation. The page content inside keeps its markup and selectors.
 */
export function Layout({ children, title, breadcrumbs }: Readonly<LayoutProps>) {
  return (
    <StoreLayout title={title} breadcrumbs={breadcrumbs}>
      <div className="row">
        <aside className="col-lg-3">
          <AccountNav />
        </aside>
        <div className="col-lg-9">{children}</div>
      </div>
    </StoreLayout>
  );
}
