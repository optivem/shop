import { useEffect, useState, type FormEvent } from 'react';
import { Navigate, useNavigate } from 'react-router-dom';
import { LoadingSpinner, ProductImage, StoreLayout, SubmitButton } from '../components';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { priceCart } from '../features/cart/cart-lines';
import { AddressFields } from '../features/checkout/AddressFields';
import { SHIPPING_LABELS } from '../features/checkout/address-labels';
import { saveConfirmation } from '../features/checkout/confirmation-store';
import { placeCheckoutOrders } from '../features/checkout/place-checkout-orders';
import {
  EMPTY_ADDRESS,
  ESTIMATED_TAX_RATES,
  formatAddress,
  formatTaxRate,
  savedAddressOf,
  trimAddress,
  validateAddress,
  type AddressErrors,
} from '../features/checkout/shipping';
import { orderService } from '../services/order-service';
import { profileService } from '../services/profile-service';
import type { ShippingAddress } from '../types/api.types';
import { formatCents } from '../utils/money';

type AddressChoice = 'saved' | 'other';

interface ProfileState {
  saved?: ShippingAddress;
  /** Names from the sign-in account, to prefill the address form on a first order. */
  prefill: ShippingAddress;
}

/** `/checkout`: shipping address, optional coupon and the order review; places one order per cart line (UC-009). */
export function Checkout() {
  const cart = useCart();
  const { products } = useCatalog();
  const navigate = useNavigate();
  const { setError, clearNotification } = useNotificationContext();
  const [profile, setProfile] = useState<ProfileState | null>(null);
  const [choice, setChoice] = useState<AddressChoice>('other');
  const [address, setAddress] = useState<ShippingAddress>(EMPTY_ADDRESS);
  const [errors, setErrors] = useState<AddressErrors>({});
  const [saveAsDefault, setSaveAsDefault] = useState(true);
  const [couponCode, setCouponCode] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    let ignore = false;
    void profileService.viewProfile().then((result) => {
      if (ignore) return;
      // A profile that can't be read is treated like an empty one: checkout never blocks on it.
      const data = result.success ? result.data : undefined;
      const saved = data ? savedAddressOf(data) : undefined;
      const prefill = { ...EMPTY_ADDRESS, firstName: data?.firstName ?? '', lastName: data?.lastName ?? '' };
      setProfile({ saved, prefill });
      setChoice(saved ? 'saved' : 'other');
      setAddress(saved ? prefillFrom(saved) : prefill);
    });
    return () => {
      ignore = true;
    };
  }, []);

  // An empty cart has nothing to check out (3a). Not while submitting: placing every line empties it.
  if (cart.lines.length === 0 && !isSubmitting) return <Navigate to="/cart" replace />;

  const { lines, subtotalCents } = priceCart(cart.lines, products);
  const shippingAddress = choice === 'saved' && profile?.saved ? profile.saved : address;
  const taxCountry = shippingAddress.country.trim();
  const taxRate = ESTIMATED_TAX_RATES[taxCountry];
  const firstOrder = profile !== null && !profile.saved;

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    clearNotification();
    const fieldErrors = choice === 'saved' ? {} : validateAddress(address);
    setErrors(fieldErrors);
    if (Object.keys(fieldErrors).length > 0) {
      setError({ message: 'Please correct the shipping address.', fieldErrors: Object.values(fieldErrors) });
      return;
    }

    setIsSubmitting(true);
    const target = trimAddress(shippingAddress);
    const code = couponCode.trim() || undefined;
    const result = await placeCheckoutOrders(cart.lines, target, code, (sku, quantity, to, coupon) =>
      orderService.placeOrderTo(sku, quantity, to, coupon),
    );

    if (result.placed.length === 0) {
      // 8b: nothing was placed; the cart stays as it is.
      setIsSubmitting(false);
      setError({
        message: 'No order was placed.',
        fieldErrors: result.rejected.map((line) => `${line.sku}: ${line.reason}`),
      });
      return;
    }

    if (firstOrder && saveAsDefault) {
      // Best effort: the orders are placed either way.
      await profileService.updateProfile(target);
    }
    // 8a: the placed lines leave the cart and the rejected ones stay in it.
    cart.removeMany(result.placed.map((line) => line.sku));
    saveConfirmation(result);
    navigate('/checkout/confirmation', { state: result });
  };

  if (!profile) {
    return (
      <StoreLayout title="Checkout">
        <div className="market-panel">
          <LoadingSpinner message="Loading checkout..." />
        </div>
      </StoreLayout>
    );
  }

  return (
    <StoreLayout title="Checkout" breadcrumbs={[{ label: 'Home', path: '/' }, { label: 'Cart', path: '/cart' }, { label: 'Checkout' }]}>
      <h1 className="h2 mb-4">Checkout</h1>
      <form className="row g-4 align-items-start" noValidate onSubmit={(e) => void handleSubmit(e)}>
        <div className="col-lg-7 d-flex flex-column gap-4">
          <section className="market-panel checkout-section" aria-label="Shipping Address">
            <h2 className="checkout-step-title">
              <span className="checkout-step-number" aria-hidden="true">
                1
              </span>
              Shipping address
            </h2>
            {profile.saved && (
              <div className="checkout-choices">
                <div className={`form-check checkout-choice${choice === 'saved' ? ' is-selected' : ''}`}>
                  <input
                    className="form-check-input"
                    type="radio"
                    name="address-choice"
                    id="address-saved"
                    aria-label="Use Saved Address"
                    checked={choice === 'saved'}
                    onChange={() => setChoice('saved')}
                  />
                  <label className="form-check-label" htmlFor="address-saved">
                    <span className="checkout-choice-title">Use saved address</span>
                    <span className="checkout-choice-detail" aria-label="Saved Address">
                      {formatAddress(profile.saved)}
                    </span>
                  </label>
                </div>
                <div className={`form-check checkout-choice${choice === 'other' ? ' is-selected' : ''}`}>
                  <input
                    className="form-check-input"
                    type="radio"
                    name="address-choice"
                    id="address-other"
                    aria-label="Ship to a Different Address"
                    checked={choice === 'other'}
                    onChange={() => setChoice('other')}
                  />
                  <label className="form-check-label" htmlFor="address-other">
                    <span className="checkout-choice-title">Ship to a different address</span>
                    <span className="checkout-choice-detail">Enter a new address below</span>
                  </label>
                </div>
              </div>
            )}
            {choice === 'other' && (
              <>
                <AddressFields
                  idPrefix="shipping"
                  address={address}
                  errors={errors}
                  labels={SHIPPING_LABELS}
                  onChange={setAddress}
                />
                {firstOrder && (
                  <div className="form-check checkout-save-default">
                    <input
                      className="form-check-input"
                      type="checkbox"
                      id="save-as-default"
                      aria-label="Save as Default Address"
                      checked={saveAsDefault}
                      onChange={(e) => setSaveAsDefault(e.target.checked)}
                    />
                    <label className="form-check-label" htmlFor="save-as-default">
                      Save as my default shipping address
                    </label>
                  </div>
                )}
              </>
            )}
          </section>
          <section className="market-panel checkout-section" aria-labelledby="checkout-coupon-heading">
            <h2 className="checkout-step-title" id="checkout-coupon-heading">
              <span className="checkout-step-number" aria-hidden="true">
                2
              </span>
              Coupon <span className="checkout-step-hint">(optional)</span>
            </h2>
            <label htmlFor="checkout-coupon" className="form-label market-label">
              Code
            </label>
            <input
              id="checkout-coupon"
              type="text"
              className="form-control checkout-coupon-input"
              aria-label="Checkout Coupon Code"
              aria-describedby="checkout-coupon-hint"
              value={couponCode}
              onChange={(e) => setCouponCode(e.target.value)}
            />
            <p id="checkout-coupon-hint" className="form-text mb-0">
              The code is checked for each order when you place it.
            </p>
          </section>
        </div>
        <div className="col-lg-5">
          <section className="market-panel checkout-section checkout-review" aria-label="Order Review">
            <h2 className="h5 fw-bold mb-0">Order review</h2>
            <ul className="checkout-review-lines">
              {lines.map((line) => (
                <li key={line.sku} className="checkout-review-line" aria-label={`Review Line ${line.sku}`}>
                  <div className="checkout-review-thumb">
                    <ProductImage brand={line.product?.brand ?? line.sku} shape="square" />
                  </div>
                  <div className="checkout-review-name">
                    <span className="checkout-review-title">{line.product?.title ?? line.sku}</span>
                    <span className="checkout-review-meta">
                      Qty <span aria-label={`Review Quantity ${line.sku}`}>{line.quantity}</span>
                    </span>
                  </div>
                  <span className="price checkout-review-total" aria-label={`Review Line Total ${line.sku}`}>
                    {formatCents(line.lineTotalCents)}
                  </span>
                </li>
              ))}
            </ul>
            <div className="checkout-review-summary">
              <div className="d-flex justify-content-between fw-bold">
                <span>Subtotal</span>
                <span className="price" aria-label="Review Subtotal">
                  {formatCents(subtotalCents)}
                </span>
              </div>
              <div className="d-flex justify-content-between">
                <span className="text-body-secondary">
                  Est. tax (<span aria-label="Tax Country">{taxCountry || '-'}</span>)
                </span>
                <span aria-label="Estimated Tax Rate">{taxRate === undefined ? '-' : formatTaxRate(taxRate)}</span>
              </div>
            </div>
            <SubmitButton
              isSubmitting={isSubmitting}
              text="Place order"
              loadingText="Placing order..."
              ariaLabel="Place Checkout Order"
              className="btn btn-primary btn-lg w-100"
            />
            <p className="text-body-secondary small text-center mb-0">
              Discount and tax are worked out per order when it is placed.
            </p>
          </section>
        </div>
      </form>
    </StoreLayout>
  );
}

function prefillFrom(saved: ShippingAddress): ShippingAddress {
  // "Ship to a different address" starts from the customer's own names, with the address left to fill in.
  return { ...EMPTY_ADDRESS, firstName: saved.firstName, lastName: saved.lastName };
}
