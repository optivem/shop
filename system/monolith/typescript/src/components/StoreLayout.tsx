"use client";

import { useEffect, type ReactNode } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { categoriesOf } from "@/lib/storefront/catalog-query";
import { Icon } from "./Icon";
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

/**
 * The storefront's notification banner, with the same markup as the order pages' own notifications.
 * The UI drivers read the success text as the element's whole textContent, so the only extra child is a
 * text-free, aria-hidden SVG icon.
 */
function Notification() {
  const { successMessage, error, notificationId } = useNotification();
  if (error) {
    return (
      <div className="notification error" role="alert" data-notification-id={notificationId}>
        <Icon name="circle-alert" size={20} className="notification-icon" />
        <div className="notification-body">
          <div className="error-message">{error.message}</div>
          {error.fieldErrors?.map((fieldError) => (
            <div key={fieldError} className="field-error">
              {fieldError}
            </div>
          ))}
        </div>
      </div>
    );
  }
  if (successMessage) {
    return (
      <div className="notification success" role="alert" data-notification-id={notificationId}>
        <Icon name="circle-check" size={20} className="notification-icon" />
        {successMessage}
      </div>
    );
  }
  return null;
}

/** Static footer: Shop, Help, the copyright note (storefront-ux §3). */
function StoreFooter() {
  const { products } = useCatalog();
  return (
    <footer className="store-footer mt-5">
      <div className="container py-4">
        <div className="row g-4">
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">Shop</h2>
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
          <div className="col-6 col-md-4">
            <h2 className="store-footer-heading">Help</h2>
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
            <p className="store-footer-brand mb-1">
              My<span className="store-logo-mark">Shop</span>
            </p>
            <p className="mb-0">© Valentina Jemuović, Optivem</p>
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

/**
 * The storefront chrome around every page: header, breadcrumbs, the notification banner and the footer.
 *
 * The header is deliberately not in a Suspense boundary of its own. Every route is dynamic (the root layout reads
 * cookies), so its useSearchParams() never bails out to client rendering and needs no boundary; and one would let
 * React stream the header as a separate segment (a hidden div swapped in by a later, batched reveal script). A cart
 * change before that reveal (an "Add to cart" click on the already-shown page) client-renders the pending boundary
 * and orphans the streamed copy, leaving a second, stale "Cart Item Count" badge in the DOM.
 */
export function StoreLayout({ children, title, breadcrumbs }: Readonly<StoreLayoutProps>) {
  useEffect(() => {
    document.title = title ? `${title} · MyShop` : "MyShop";
  }, [title]);

  return (
    <div className="store d-flex flex-column min-vh-100">
      <StoreHeader />
      <main className="container store-main flex-grow-1">
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
      <h2 className="account-nav-heading">{session.isAdmin ? "Admin" : "My account"}</h2>
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
      <div className="row g-4">
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
      {message && <p className="mt-3 mb-0 text-body-secondary fw-semibold">{message}</p>}
    </div>
  );
}

/**
 * A load error: Bootstrap's `.alert-danger` with an "Error:" prefix (the UI drivers read both) and an optional
 * retry button. The icon is aria-hidden and carries no text.
 */
export function ErrorAlert({ message, onRetry }: Readonly<{ message: string; onRetry?: () => void }>) {
  return (
    <div className="alert alert-danger d-flex flex-wrap justify-content-between align-items-center gap-2" role="alert">
      <div className="d-flex align-items-start gap-2">
        <Icon name="circle-alert" size={18} className="flex-shrink-0 mt-1" />
        <div>
          <strong>Error:</strong> {message}
        </div>
      </div>
      {onRetry && (
        <button type="button" className="btn btn-outline-danger" onClick={onRetry}>
          Try Again
        </button>
      )}
    </div>
  );
}
