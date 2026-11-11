import { useEffect, type ReactNode } from 'react';
import { StoreHeader } from './StoreHeader';
import { StoreFooter } from './StoreFooter';
import { Breadcrumb } from './Breadcrumb';
import { Notification } from './Notification';
import { useNotificationContext } from '../contexts/useNotificationContext';

export interface BreadcrumbItem {
  label: string;
  path?: string;
}

export interface StoreLayoutProps {
  children: ReactNode;
  /** Shown in the browser tab. */
  title?: string;
  breadcrumbs?: BreadcrumbItem[];
}

/** The storefront chrome around every page: header, breadcrumbs, the notification banner and the footer. */
export function StoreLayout({ children, title, breadcrumbs }: Readonly<StoreLayoutProps>) {
  const { successMessage, error, notificationId } = useNotificationContext();

  useEffect(() => {
    document.title = title ? `${title} · MyShop` : 'MyShop';
  }, [title]);

  return (
    <div className="store d-flex flex-column min-vh-100">
      <StoreHeader />
      <main className="container store-main flex-grow-1">
        {breadcrumbs && <Breadcrumb items={breadcrumbs} />}
        <Notification successMessage={successMessage} error={error} notificationId={notificationId} />
        {children}
      </main>
      <StoreFooter />
    </div>
  );
}
