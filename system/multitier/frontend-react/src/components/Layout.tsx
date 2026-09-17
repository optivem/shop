import { ReactNode } from 'react';
import { Navbar } from './Navbar';
import { Breadcrumb } from './Breadcrumb';
import { Notification } from './Notification';
import { useNotificationContext } from '../contexts/useNotificationContext';

interface BreadcrumbItem {
  label: string;
  path?: string;
}

interface LayoutProps {
  children: ReactNode;
  title?: string;
  breadcrumbs?: BreadcrumbItem[];
}

/**
 * Layout wrapper component that provides consistent page structure
 * Includes navbar, notification, and optional breadcrumbs for all pages
 * @param children - Page content to render
 * @param title - Optional page title displayed in navbar
 * @param breadcrumbs - Optional breadcrumb navigation items
 */
export function Layout({ children, title, breadcrumbs }: Readonly<LayoutProps>) {
  const { successMessage, error, notificationId } = useNotificationContext();

  return (
    <>
      <Navbar title={title} />
      <div className="container mt-4">
        {breadcrumbs && <Breadcrumb items={breadcrumbs} />}
        <Notification successMessage={successMessage} error={error} notificationId={notificationId} />
        {children}
      </div>
    </>
  );
}
