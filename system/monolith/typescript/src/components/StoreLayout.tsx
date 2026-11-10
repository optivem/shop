"use client";

import { Suspense, useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { categoriesOf } from "@/lib/storefront/catalog-query";
import { StoreHeader } from "./StoreHeader";
import { useCatalog, useNotification, useSession } from "./StorefrontProviders";

export interface BreadcrumbItem {
  label: string;
  path?: string;
}

/** Breadcrumb navigation showing the page hierarchy. */
export function Breadcrumb({ items }: Readonly<{ items: BreadcrumbItem[] }>) {
  return (
    <nav aria-label="breadcrumb">
      <ol className="breadcrumb">
        {items.map((item, index) => (
          <li
            key={item.path ?? item.label}
            className={`breadcrumb-item ${index === items.length - 1 ? "active" : ""}`}
            aria-current={index === items.length - 1 ? "page" : undefined}
          >
            {item.path ? <Link href={item.path}>{item.label}</Link> : item.label}
          </li>
        ))}
      </ol>
    </nav>
  );
}

/** The storefront's notification banner, with the same markup as the order pages' own notifications. */
function Notification() {
  const { successMessage, error, notificationId } = useNotification();
  if (error) {
    return (
      <div className="notification error" role="alert" data-notification-id={notificationId}>
        <div className="error-message">{error.message}</div>
        {error.fieldErrors?.map((fieldError) => (
          <div key={fieldError} className="field-error">
            {fieldError}
          </div>
        ))}
      </div>
    );
  }
  if (successMessage) {
    return (
      <div className="notification success" role="alert" data-notification-id={notificationId}>
        {successMessage}
      </div>
    );
  }
  return null;
}

/** Static footer: Shop, Help, and the demo-store note (storefront-ux §3). */
function StoreFooter() {
  const { products } = useCatalog();
  return (
    <footer className="store-footer mt-5">
      <div className="container py-4">
        <div className="row g-4">
          <div className="col-md-4">
            <h2 className="h6">Shop</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <Link href="/">All products</Link>
              </li>
              {categoriesOf(products).map((category) => (
                <li key={category}>
                  <Link href={`/?category=${encodeURIComponent(category)}`}>{category}</Link>
                </li>
              ))}
            </ul>
          </div>
          <div className="col-md-4">
            <h2 className="h6">Help</h2>
            <ul className="list-unstyled mb-0">
              <li>
                <a href="#shipping">Shipping</a>
              </li>
              <li>
                <a href="#returns">Returns</a>
              </li>
              <li>
                <a href="#contact">Contact</a>
              </li>
            </ul>
          </div>
          <div className="col-md-4 text-md-end">
            <p className="mb-0">© MyShop · demo store for the Optivem ATDD course</p>
          </div>
        </div>
      </div>
    </footer>
  );
}

export interface StoreLayoutProps {
  children: ReactNode;
  /** Shown in the browser tab. */
  title?: string;
  breadcrumbs?: BreadcrumbItem[];
}

/** The storefront chrome around every page: header, breadcrumbs, the notification banner and the footer. */
export function StoreLayout({ children, title, breadcrumbs }: Readonly<StoreLayoutProps>) {
  useEffect(() => {
    document.title = title ? `${title} · MyShop` : "MyShop";
  }, [title]);

  return (
    <div className="store d-flex flex-column min-vh-100">
      <Suspense>
        <StoreHeader />
      </Suspense>
      <main className="container mt-4 flex-grow-1">
        {breadcrumbs && <Breadcrumb items={breadcrumbs} />}
        <Notification />
        {children}
      </main>
      <StoreFooter />
    </div>
  );
}

/**
 * The side navigation of the signed-in area: "My account" for customers, "Admin" for admins (storefront-ux §8).
 * Admin › All orders is the same /order-history page customers see as My orders.
 */
function AccountNav() {
  const session = useSession();
  const pathname = usePathname();
  if (!session) return null;

  const linkClass = (path: string) => `list-group-item list-group-item-action${pathname === path ? " active" : ""}`;

  return (
    <nav className="account-nav mb-4" aria-label={session.isAdmin ? "Admin" : "My account"}>
      <h2 className="h6 text-uppercase text-muted">{session.isAdmin ? "Admin" : "My account"}</h2>
      <div className="list-group">
        {session.isAdmin ? (
          <>
            <Link className={linkClass("/order-history")} href="/order-history">
              All orders
            </Link>
            <Link className={linkClass("/admin-coupons")} href="/admin-coupons">
              Coupons
            </Link>
          </>
        ) : (
          <>
            <Link className={linkClass("/order-history")} href="/order-history">
              My orders
            </Link>
            <Link className={linkClass("/account")} href="/account">
              Profile &amp; address
            </Link>
            {session.canPlaceOrder && (
              <Link className={linkClass("/new-order")} href="/new-order">
                Quick order
              </Link>
            )}
          </>
        )}
      </div>
    </nav>
  );
}

/**
 * Layout of the signed-in area (Quick order, My orders, order details, admin pages, My account): the store
 * chrome plus the account or admin side navigation. The page content inside keeps its markup and selectors.
 */
export function AccountLayout({ children, title, breadcrumbs }: Readonly<StoreLayoutProps>) {
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

export function LoadingSpinner({ message = "Loading..." }: Readonly<{ message?: string }>) {
  return (
    <div className="text-center py-5">
      <output>
        <div className="spinner-border text-primary">
          <span className="visually-hidden">Loading...</span>
        </div>
      </output>
      {message && <p className="mt-3">{message}</p>}
    </div>
  );
}
