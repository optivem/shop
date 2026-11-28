import type { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ProductImage } from '../../components';
import { useCatalog } from '../../contexts/useCatalog';
import { formatDateTime, formatPercent } from '../../i18n/format';
import { formatAddress } from '../checkout/shipping';
import type { ViewOrderDetailsResponse } from '../../types/api.types';

export interface OrderDetailViewProps {
  order: ViewOrderDetailsResponse;
  /** The Cancel / Deliver buttons, shown next to the order number. */
  actions?: ReactNode;
}

/**
 * Order detail view: a header with the order number, date and status, the ordered item, and the price breakdown.
 * Every value the drivers read sits alone in its `Display <Field>` element, exactly as the API returned it.
 */
export function OrderDetailView({ order, actions }: Readonly<OrderDetailViewProps>) {
  const { t } = useTranslation('account');
  // The order only carries the SKU; the catalog (when loaded) gives the title and brand for the item row.
  const product = useCatalog().products.find((candidate) => candidate.sku === order.sku);
  const timestamp = formatDateTime(order.orderTimestamp);

  return (
    <>
      <section className="market-panel order-detail-header">
        <div className="min-w-0">
          <p className="order-eyebrow mb-1">{t('orderDetailView.eyebrow')}</p>
          <h1 className="order-detail-number">
            <span aria-label={t('orderDetailView.displayOrderNumber')}>{order.orderNumber}</span>
          </h1>
          <p className="text-body-secondary mb-0 d-flex flex-wrap align-items-center gap-2">
            <span>
              {t('orderDetailView.ordered')} <span aria-label={t('orderDetailView.displayOrderTimestamp')}>{timestamp}</span> UTC
            </span>
            <span className={`order-status status-${order.status}`} aria-label={t('orderDetailView.displayStatus')}>{order.status}</span>
          </p>
        </div>
        {actions && <div className="order-detail-actions">{actions}</div>}
      </section>

      <div className="row g-4 align-items-start">
        <div className="col-xl-7">
          <section className="market-panel p-4" aria-labelledby="order-item-heading">
            <h2 className="h5 mb-3" id="order-item-heading">
              {t('orderDetailView.item')}
            </h2>
            <div className="order-item">
              <div className="order-item-thumb">
                <ProductImage imageUrl={product?.imageUrl} alt="" shape="square" />
              </div>
              <div className="min-w-0">
                {product && <p className="order-item-title mb-0">{product.title}</p>}
                <p className="order-item-meta mb-0">
                  {t('orderDetailView.sku')} <span className="font-monospace" aria-label={t('orderDetailView.displaySku')}>{order.sku}</span> · {t('orderDetailView.quantity')}{' '}
                  <span aria-label={t('orderDetailView.displayQuantity')}>{order.quantity}</span> · {t('orderDetailView.unitPrice')}{' '}
                  <span aria-label={t('orderDetailView.displayUnitPrice')}>${order.unitPrice}</span>
                </p>
              </div>
            </div>
            <dl className="order-facts">
              {order.shippingAddress && (
                <div>
                  <dt>{t('orderDetailView.shippingAddress')}</dt>
                  <dd aria-label={t('orderDetailView.displayShippingAddress')}>{formatAddress(order.shippingAddress)}</dd>
                </div>
              )}
              <div>
                <dt>{t('orderDetailView.country')}</dt>
                <dd aria-label={t('orderDetailView.displayCountry')}>{order.country}</dd>
              </div>
              <div>
                <dt>{t('orderDetailView.appliedCoupon')}</dt>
                <dd className="font-monospace" aria-label={t('orderDetailView.displayAppliedCoupon')}>{order.appliedCouponCode ?? t('orderDetailView.none')}</dd>
              </div>
            </dl>
          </section>
        </div>
        <div className="col-xl-5">
          <section className="market-panel p-4" aria-labelledby="order-price-heading">
            <h2 className="h5 mb-3" id="order-price-heading">
              {t('orderDetailView.priceBreakdown')}
            </h2>
            <dl className="order-breakdown">
              <dt>{t('orderDetailView.basePrice')}</dt>
              <dd aria-label={t('orderDetailView.displayBasePrice')}>${order.basePrice}</dd>
              <dt>{t('orderDetailView.discountRate')}</dt>
              <dd aria-label={t('orderDetailView.displayDiscountRate')}>{formatPercent(order.discountRate)}</dd>
              <dt>{t('orderDetailView.discountAmount')}</dt>
              <dd aria-label={t('orderDetailView.displayDiscountAmount')}>${order.discountAmount}</dd>
              <dt>{t('orderDetailView.subtotalPrice')}</dt>
              <dd className="fw-bold" aria-label={t('orderDetailView.displaySubtotalPrice')}>${order.subtotalPrice}</dd>
              <dt>{t('orderDetailView.taxRate')}</dt>
              <dd aria-label={t('orderDetailView.displayTaxRate')}>{formatPercent(order.taxRate)}</dd>
              <dt>{t('orderDetailView.taxAmount')}</dt>
              <dd aria-label={t('orderDetailView.displayTaxAmount')}>${order.taxAmount}</dd>
            </dl>
            <div className="order-total-row">
              <span>{t('orderDetailView.totalPrice')}</span>
              <span className="order-total-value" aria-label={t('orderDetailView.displayTotalPrice')}>${order.totalPrice}</span>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
