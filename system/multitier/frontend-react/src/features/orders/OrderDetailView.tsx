import type { ReactNode } from 'react';
import { ProductImage } from '../../components';
import { useCatalog } from '../../contexts/useCatalog';
import { formatAddress } from '../checkout/shipping';
import type { ViewOrderDetailsResponse } from '../../types/api.types';

export interface OrderDetailViewProps {
  order: ViewOrderDetailsResponse;
  /** The Cancel / Deliver buttons, shown next to the order number. */
  actions?: ReactNode;
}

const percent = (rate: string) => `${(Number(rate) * 100).toFixed(2)}%`;

/**
 * Order detail view: a header with the order number, date and status, the ordered item, and the price breakdown.
 * Every value the drivers read sits alone in its `Display <Field>` element, exactly as the API returned it.
 */
export function OrderDetailView({ order, actions }: Readonly<OrderDetailViewProps>) {
  // The order only carries the SKU; the catalog (when loaded) gives the title and brand for the item row.
  const product = useCatalog().products.find((candidate) => candidate.sku === order.sku);
  const timestamp = new Date(order.orderTimestamp).toLocaleString('en-US', { timeZone: 'UTC' });

  return (
    <>
      <section className="market-panel order-detail-header">
        <div className="min-w-0">
          <p className="order-eyebrow mb-1">Order Details</p>
          <h1 className="order-detail-number">
            <span aria-label="Display Order Number">{order.orderNumber}</span>
          </h1>
          <p className="text-body-secondary mb-0 d-flex flex-wrap align-items-center gap-2">
            <span>
              Ordered <span aria-label="Display Order Timestamp">{timestamp}</span> UTC
            </span>
            <span className={`order-status status-${order.status}`} aria-label="Display Status">{order.status}</span>
          </p>
        </div>
        {actions && <div className="order-detail-actions">{actions}</div>}
      </section>

      <div className="row g-4 align-items-start">
        <div className="col-xl-7">
          <section className="market-panel p-4" aria-labelledby="order-item-heading">
            <h2 className="h5 mb-3" id="order-item-heading">
              Item
            </h2>
            <div className="order-item">
              <div className="order-item-thumb">
                <ProductImage brand={product?.brand ?? order.sku} shape="square" />
              </div>
              <div className="min-w-0">
                {product && <p className="order-item-title mb-0">{product.title}</p>}
                <p className="order-item-meta mb-0">
                  SKU <span className="font-monospace" aria-label="Display SKU">{order.sku}</span> · Quantity{' '}
                  <span aria-label="Display Quantity">{order.quantity}</span> · Unit Price{' '}
                  <span aria-label="Display Unit Price">${order.unitPrice}</span>
                </p>
              </div>
            </div>
            <dl className="order-facts">
              {order.shippingAddress && (
                <div>
                  <dt>Shipping Address</dt>
                  <dd aria-label="Display Shipping Address">{formatAddress(order.shippingAddress)}</dd>
                </div>
              )}
              <div>
                <dt>Country</dt>
                <dd aria-label="Display Country">{order.country}</dd>
              </div>
              <div>
                <dt>Applied Coupon</dt>
                <dd className="font-monospace" aria-label="Display Applied Coupon">{order.appliedCouponCode ?? 'None'}</dd>
              </div>
            </dl>
          </section>
        </div>
        <div className="col-xl-5">
          <section className="market-panel p-4" aria-labelledby="order-price-heading">
            <h2 className="h5 mb-3" id="order-price-heading">
              Price breakdown
            </h2>
            <dl className="order-breakdown">
              <dt>Base Price</dt>
              <dd aria-label="Display Base Price">${order.basePrice}</dd>
              <dt>Discount Rate</dt>
              <dd aria-label="Display Discount Rate">{percent(order.discountRate)}</dd>
              <dt>Discount Amount</dt>
              <dd aria-label="Display Discount Amount">${order.discountAmount}</dd>
              <dt>Subtotal Price</dt>
              <dd className="fw-bold" aria-label="Display Subtotal Price">${order.subtotalPrice}</dd>
              <dt>Tax Rate</dt>
              <dd aria-label="Display Tax Rate">{percent(order.taxRate)}</dd>
              <dt>Tax Amount</dt>
              <dd aria-label="Display Tax Amount">${order.taxAmount}</dd>
            </dl>
            <div className="order-total-row">
              <span>Total Price</span>
              <span className="order-total-value" aria-label="Display Total Price">${order.totalPrice}</span>
            </div>
          </section>
        </div>
      </div>
    </>
  );
}
