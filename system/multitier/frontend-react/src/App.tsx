import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { useEffect, type ReactNode } from 'react';
import { ErrorBoundary } from './components';
import { NotificationProvider } from './contexts/NotificationContext';
import { useNotificationContext } from './contexts/useNotificationContext';
import { CartProvider } from './contexts/CartContext';
import { CatalogProvider } from './contexts/CatalogContext';
import { RequireAuth } from './auth/AuthGate';
import { RequireAdmin } from './auth/RequireAdmin';
import { RequireCanPlaceOrder } from './auth/RequireCanPlaceOrder';
import {
  Account,
  AdminCoupons,
  Cart,
  Catalog,
  Checkout,
  OrderConfirmation,
  OrderDetails,
  OrderHistory,
  ProductDetail,
  QuickOrder,
  SignIn,
} from './pages';

/**
 * Component that clears notifications on route change
 */
function RouteChangeHandler() {
  const location = useLocation();
  const { clearNotification } = useNotificationContext();

  useEffect(() => {
    clearNotification();
  }, [location.pathname, clearNotification]);

  return null;
}

/** Signed-in customers only: anonymous visitors go to the login form, admins see "Not authorized". */
const customer = (page: ReactNode) => (
  <RequireAuth>
    <RequireCanPlaceOrder>{page}</RequireCanPlaceOrder>
  </RequireAuth>
);

const signedIn = (page: ReactNode) => <RequireAuth>{page}</RequireAuth>;

const admin = (page: ReactNode) => (
  <RequireAuth>
    <RequireAdmin>{page}</RequireAdmin>
  </RequireAuth>
);

/**
 * Main application component with routing configuration (storefront-ux §2).
 * The storefront (catalog, product pages, cart) is public; the rest wraps itself in RequireAuth.
 * Wrapped in ErrorBoundary for graceful error handling; the providers hold the notification, catalog and cart state.
 */
export function App() {
  return (
    <ErrorBoundary>
      <NotificationProvider>
        <CatalogProvider>
          <CartProvider>
            <BrowserRouter>
              <RouteChangeHandler />
              <Routes>
                <Route path="/" element={<Catalog />} />
                <Route path="/products/:sku" element={<ProductDetail />} />
                <Route path="/cart" element={<Cart />} />
                <Route path="/sign-in" element={<SignIn />} />
                <Route path="/checkout" element={customer(<Checkout />)} />
                <Route path="/checkout/confirmation" element={customer(<OrderConfirmation />)} />
                <Route path="/new-order" element={customer(<QuickOrder />)} />
                <Route path="/order-history" element={signedIn(<OrderHistory />)} />
                <Route path="/order-details/:orderNumber" element={signedIn(<OrderDetails />)} />
                <Route path="/account" element={signedIn(<Account />)} />
                <Route path="/admin-coupons" element={admin(<AdminCoupons />)} />
              </Routes>
            </BrowserRouter>
          </CartProvider>
        </CatalogProvider>
      </NotificationProvider>
    </ErrorBoundary>
  );
}
