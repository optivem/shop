import { Link, useLocation } from 'react-router-dom';
import { CircleCheck, PackageSearch, TriangleAlert } from 'lucide-react';
import { StoreLayout } from '../components';
import { useCatalog } from '../contexts/useCatalog';
import { loadConfirmation } from '../features/checkout/confirmation-store';

/** `/checkout/confirmation`: the orders just placed (and, after a partial failure, the rejected lines). */
export function OrderConfirmation() {
  const location = useLocation();
  const result = loadConfirmation(location.state);
  const { products } = useCatalog();
  const titleOf = (sku: string) => products.find((product) => product.sku === sku)?.title;
  const placedCount = result?.placed.length ?? 0;
  const rejectedCount = result?.rejected.length ?? 0;

  return (
    <StoreLayout title="Order confirmation" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Order confirmation' }]}>
      <div className="order-confirmation">
        {result && placedCount > 0 ? (
          <>
            <section className="market-panel order-confirmation-hero">
              <div className="order-confirmation-icon" aria-hidden="true">
                <CircleCheck size={32} />
              </div>
              <h1 className="h2 mb-1">Thank you for your order!</h1>
              <p className="text-body-secondary mb-0">
                {placedCount === 1 ? 'Your order has been placed:' : `Your ${placedCount} orders have been placed:`}
              </p>
            </section>
            <section className="market-panel p-4" aria-labelledby="confirmed-orders-heading">
              <h2 className="h6 fw-bold mb-3" id="confirmed-orders-heading">
                Confirmed orders
              </h2>
              <ul className="confirmed-orders" aria-label="Confirmed Orders">
                {result.placed.map((line) => (
                  <li key={line.orderNumber} className="confirmed-order">
                    <div className="min-w-0">
                      <Link
                        to={`/order-details/${encodeURIComponent(line.orderNumber)}`}
                        className="order-number-link"
                        aria-label={`Confirmed Order ${line.orderNumber}`}
                      >
                        {line.orderNumber}
                      </Link>
                      <p className="confirmed-order-meta mb-0">
                        {titleOf(line.sku) ?? line.sku} × {line.quantity}
                        {titleOf(line.sku) && <span className="font-monospace"> · {line.sku}</span>}
                      </p>
                    </div>
                    <span className="order-status status-PLACED">PLACED</span>
                  </li>
                ))}
              </ul>
            </section>
          </>
        ) : (
          <section className="market-empty text-center py-5 px-3">
            <PackageSearch size={40} aria-hidden="true" className="mb-3 text-body-secondary" />
            <h1 className="h3">No recent order</h1>
            <p className="mb-0">Orders you place at checkout show up here right after.</p>
          </section>
        )}
        {result && rejectedCount > 0 && (
          <div className="alert alert-warning order-rejected mb-0" aria-label="Rejected Lines">
            <h2 className="h6 fw-bold d-flex align-items-center gap-2 mb-2">
              <TriangleAlert size={18} aria-hidden="true" />
              {rejectedCount === 1 ? '1 item could not be ordered' : `${rejectedCount} items could not be ordered`}
            </h2>
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
        <div className="order-confirmation-actions">
          <Link to="/order-history" className="btn btn-primary btn-lg">
            View my orders
          </Link>
          <Link to="/" className="btn btn-outline-secondary btn-lg">
            Continue shopping
          </Link>
        </div>
      </div>
    </StoreLayout>
  );
}
