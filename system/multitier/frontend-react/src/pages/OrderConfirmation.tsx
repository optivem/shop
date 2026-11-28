import { useTranslation } from 'react-i18next';
import { Link, useLocation } from 'react-router-dom';
import { CircleCheck, PackageSearch, TriangleAlert } from 'lucide-react';
import { CheckoutProgress, ProductImage, StoreLayout } from '../components';
import { useCatalog } from '../contexts/useCatalog';
import { loadConfirmation } from '../features/checkout/confirmation-store';

/** `/checkout/confirmation`: the orders just placed (and, after a partial failure, the rejected lines). */
export function OrderConfirmation() {
  const { t } = useTranslation('checkout');
  const location = useLocation();
  const result = loadConfirmation(location.state);
  const { products } = useCatalog();
  const productOf = (sku: string) => products.find((product) => product.sku === sku);
  const titleOf = (sku: string) => productOf(sku)?.title;
  const placedCount = result?.placed.length ?? 0;
  const rejectedCount = result?.rejected.length ?? 0;

  return (
    <StoreLayout
      title={t('nav.orderConfirmation')}
      breadcrumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.orderConfirmation') }]}
    >
      <div className="order-confirmation">
        {result && placedCount > 0 ? (
          <>
            <CheckoutProgress current="Confirmation" />
            <section className="market-panel order-confirmation-hero">
              <div className="order-confirmation-icon" aria-hidden="true">
                <CircleCheck size={32} />
              </div>
              <h1 className="h2 mb-1">{t('confirmation.thanks')}</h1>
              <p className="text-body-secondary mb-0">
                {t('confirmation.placed', { count: placedCount })}
              </p>
            </section>
            <section className="market-panel p-4" aria-labelledby="confirmed-orders-heading">
              <h2 className="h6 fw-bold mb-3" id="confirmed-orders-heading">
                {t('confirmation.confirmedOrders')}
              </h2>
              <ul className="confirmed-orders" aria-label={t('confirmation.confirmedOrdersLabel')}>
                {result.placed.map((line) => (
                  <li key={line.orderNumber} className="confirmed-order">
                    <div className="confirmed-order-thumb">
                      <ProductImage imageUrl={productOf(line.sku)?.imageUrl} alt="" shape="square" />
                    </div>
                    <div className="min-w-0 flex-grow-1">
                      <Link
                        to={`/order-details/${encodeURIComponent(line.orderNumber)}`}
                        className="order-number-link"
                        aria-label={t('confirmation.confirmedOrderLabel', { orderNumber: line.orderNumber })}
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
            <h1 className="h3">{t('confirmation.noRecent')}</h1>
            <p className="mb-0">{t('confirmation.noRecentHint')}</p>
          </section>
        )}
        {result && rejectedCount > 0 && (
          <div className="alert alert-warning order-rejected mb-0" aria-label={t('confirmation.rejectedLabel')}>
            <h2 className="h6 fw-bold d-flex align-items-center gap-2 mb-2">
              <TriangleAlert size={18} aria-hidden="true" />
              {t('confirmation.rejectedHeading', { count: rejectedCount })}
            </h2>
            <p className="mb-2">{t('confirmation.rejectedIntro')}</p>
            <ul className="mb-0">
              {result.rejected.map((line) => (
                <li key={line.sku} aria-label={t('confirmation.rejectedLineLabel', { sku: line.sku })}>
                  <strong>{line.sku}</strong>: <span aria-label={t('confirmation.rejectionReasonLabel', { sku: line.sku })}>{line.reason}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
        <div className="order-confirmation-actions">
          <Link to="/order-history" className="btn btn-primary btn-lg">
            {t('confirmation.viewOrders')}
          </Link>
          <Link to="/" className="btn btn-outline-secondary btn-lg">
            {t('confirmation.continueShopping')}
          </Link>
        </div>
      </div>
    </StoreLayout>
  );
}
