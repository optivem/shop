import { Link, useLocation } from 'react-router-dom';
import { StoreLayout } from '../components';
import { loadConfirmation } from '../features/checkout/confirmation-store';

/** `/checkout/confirmation`: the orders just placed (and, after a partial failure, the rejected lines). */
export function OrderConfirmation() {
  const location = useLocation();
  const result = loadConfirmation(location.state);

  return (
    <StoreLayout title="Order confirmation" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Order confirmation' }]}>
      <div className="card shadow-sm">
        <div className="card-body">
          {result && result.placed.length > 0 ? (
            <>
              <h1 className="h3">Thank you for your order!</h1>
              <p>
                {result.placed.length === 1 ? 'Your order has been placed:' : `Your ${result.placed.length} orders have been placed:`}
              </p>
              <ul aria-label="Confirmed Orders">
                {result.placed.map((line) => (
                  <li key={line.orderNumber}>
                    <Link
                      to={`/order-details/${encodeURIComponent(line.orderNumber)}`}
                      aria-label={`Confirmed Order ${line.orderNumber}`}
                    >
                      {line.orderNumber}
                    </Link>{' '}
                    <span className="text-muted">
                      ({line.sku} × {line.quantity})
                    </span>
                  </li>
                ))}
              </ul>
            </>
          ) : (
            <h1 className="h3">No recent order</h1>
          )}
          {result && result.rejected.length > 0 && (
            <div className="alert alert-warning" aria-label="Rejected Lines">
              <p className="mb-2">These items were not ordered and are still in your cart:</p>
              <ul className="mb-0">
                {result.rejected.map((line) => (
                  <li key={line.sku} aria-label={`Rejected Line ${line.sku}`}>
                    <strong>{line.sku}</strong>: <span aria-label={`Rejection Reason ${line.sku}`}>{line.reason}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
          <div className="d-flex gap-3 mt-3">
            <Link to="/order-history" className="btn btn-primary">
              View my orders
            </Link>
            <Link to="/" className="btn btn-outline-secondary">
              Continue shopping
            </Link>
          </div>
        </div>
      </div>
    </StoreLayout>
  );
}
