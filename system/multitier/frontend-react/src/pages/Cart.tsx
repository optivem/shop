import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ShoppingCart, Trash2 } from 'lucide-react';
import { CheckoutProgress, LoadingSpinner, ProductImage, QuantityStepper, StoreLayout } from '../components';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { priceCart } from '../features/cart/cart-lines';
import { formatCents, formatMoney } from '../utils/money';

/** `/cart`: the cart lines at current catalog prices, with quantity changes, removal and the subtotal (UC-008). */
export function Cart() {
  const { t } = useTranslation('checkout');
  const cart = useCart();
  const { products, isLoading, error, reload } = useCatalog();
  const { setError, clearNotification } = useNotificationContext();

  // Prices are re-read from the catalog whenever the cart is shown.
  useEffect(() => {
    reload();
  }, [reload]);

  const { lines, subtotalCents } = priceCart(cart.lines, products);
  const itemCount = cart.lines.length;

  const renderContent = () => {
    if (itemCount === 0) {
      return (
        <div className="market-empty text-center py-5 px-3">
          <ShoppingCart size={40} aria-hidden="true" className="mb-3 text-body-secondary" />
          <p className="lead">{t('cart.empty')}</p>
          <Link to="/" className="btn btn-primary">
            {t('cart.startShopping')}
          </Link>
        </div>
      );
    }
    if (error) {
      return (
        <div className="alert alert-danger" role="alert">
          {t('cart.productsUnavailable')}
        </div>
      );
    }
    if (isLoading && products.length === 0) return <LoadingSpinner message={t('cart.loading')} />;
    return (
      <div className="row g-4 align-items-start">
        <div className="col-lg-8">
          <div className="market-panel px-3 px-md-4 py-2">
            <table className="table align-middle mb-0 cart-table" aria-label={t('cart.linesLabel')}>
              <thead>
                <tr>
                  <th scope="col">{t('cart.product')}</th>
                  <th scope="col">{t('cart.quantity')}</th>
                  <th scope="col" className="text-end">
                    {t('cart.lineTotal')}
                  </th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line) => (
                  <tr key={line.sku} aria-label={t('cart.lineLabel', { sku: line.sku })}>
                    <td className="cart-cell-product">
                      <div className="d-flex align-items-center gap-3">
                        <div className="cart-thumb">
                          <ProductImage imageUrl={line.product?.imageUrl} alt="" shape="square" lazy />
                        </div>
                        <div className="min-w-0">
                          {line.product ? (
                            <Link
                              to={`/products/${encodeURIComponent(line.sku)}`}
                              className="cart-line-title"
                              data-testid={`cart-line-title-${line.sku}`}
                            >
                              {line.product.title}
                            </Link>
                          ) : (
                            <span className="cart-line-title" data-testid={`cart-line-title-${line.sku}`}>
                              {line.sku}
                            </span>
                          )}
                          <div className="cart-line-meta">
                            {line.product ? (
                              <>
                                {t('cart.sku', { sku: line.sku })} ·{' '}
                                <span className="price" aria-label={t('cart.unitPriceLabel', { sku: line.sku })}>
                                  {formatMoney(line.product.price)}
                                </span>{' '}
                                {t('cart.each')}
                              </>
                            ) : (
                              <>
                                <span className="text-danger">{t('cart.noLongerAvailable')}</span> ·{' '}
                                <span aria-label={t('cart.unitPriceLabel', { sku: line.sku })}>-</span>
                              </>
                            )}
                          </div>
                          <button
                            type="button"
                            className="btn btn-link cart-remove"
                            aria-label={t('cart.removeLabel', { sku: line.sku })}
                            onClick={() => cart.remove(line.sku)}
                          >
                            <Trash2 size={14} aria-hidden="true" className="me-1" />
                            {t('cart.remove')}
                          </button>
                        </div>
                      </div>
                    </td>
                    <td className="cart-cell-quantity">
                      <QuantityStepper
                        quantity={line.quantity}
                        onChange={(quantity) => {
                          clearNotification();
                          cart.setQuantity(line.sku, quantity);
                        }}
                        onInvalid={() => setError({ message: t('cart.quantityError') })}
                        ariaLabel={t('cart.quantityFor', { sku: line.sku })}
                        decreaseLabel={t('cart.decreaseQuantity', { sku: line.sku })}
                        increaseLabel={t('cart.increaseQuantity', { sku: line.sku })}
                      />
                    </td>
                    <td className="cart-cell-total text-end price" aria-label={t('cart.lineTotalLabel', { sku: line.sku })}>
                      {formatCents(line.lineTotalCents)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="col-lg-4">
          <div className="market-panel p-4 d-flex flex-column gap-3">
            <h2 className="h5 mb-0">{t('cart.orderSummary')}</h2>
            <div className="d-flex justify-content-between cart-summary-row">
              <span>{t('cart.subtotal')}</span>
              <span className="price fw-bold" aria-label={t('cart.subtotalLabel')}>
                {formatCents(subtotalCents)}
              </span>
            </div>
            <p className="text-body-secondary small mb-0">{t('cart.taxNote')}</p>
            <Link to="/checkout" className="btn btn-cta btn-lg w-100" aria-label={t('cart.proceedLabel')}>
              {t('cart.proceed')}
            </Link>
            <Link to="/" className="cart-continue">
              {t('cart.continueShopping')}
            </Link>
          </div>
        </div>
      </div>
    );
  };

  return (
    <StoreLayout title={t('nav.cart')} breadcrumbs={[{ label: t('nav.home'), path: '/' }, { label: t('nav.cart') }]}>
      {itemCount > 0 && <CheckoutProgress current="Cart" />}
      <h1 className="h2 mb-4">
        {t('cart.heading')}{' '}
        {itemCount > 0 && (
          <span className="cart-line-count" aria-label={t('cart.lineCountLabel')}>
            ({t('cart.itemCount', { count: itemCount })})
          </span>
        )}
      </h1>
      {renderContent()}
    </StoreLayout>
  );
}
