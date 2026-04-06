import { BrowserRouter, Routes, Route, useLocation } from 'react-router-dom';
import { useEffect } from 'react';
import { ErrorBoundary } from './components';
import { NotificationProvider, useNotificationContext } from './contexts/NotificationContext';
import { Home, NewOrder, OrderHistory, OrderDetails, AdminCoupons } from './pages';

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

/**
 * Main application component with routing configuration
 * Wrapped in ErrorBoundary for graceful error handling
 * NotificationProvider provides global notification state
 */
export function App() {
  return (
    <ErrorBoundary>
      <NotificationProvider>
        <BrowserRouter>
          <RouteChangeHandler />
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/new-order" element={<NewOrder />} />
            <Route path="/order-history" element={<OrderHistory />} />
            <Route path="/order-details/:orderNumber" element={<OrderDetails />} />
            <Route path="/admin-coupons" element={<AdminCoupons />} />
          </Routes>
        </BrowserRouter>
      </NotificationProvider>
    </ErrorBoundary>
  );
}
