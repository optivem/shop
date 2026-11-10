// Checkout (UC-009) and its confirmation, with the order and profile services stubbed.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthContext } from 'react-oidc-context';
import { CartProvider } from '../../contexts/CartContext';
import { CART_STORAGE_KEY } from '../../contexts/cart-storage';
import { NotificationProvider } from '../../contexts/NotificationContext';
import { placeCheckoutOrders, reasonOf } from '../../features/checkout/place-checkout-orders';
import { formatAddress, validateAddress } from '../../features/checkout/shipping';
import { Checkout } from '../../pages/Checkout';
import { OrderConfirmation } from '../../pages/OrderConfirmation';
import { orderService } from '../../services/order-service';
import { profileService } from '../../services/profile-service';
import type { ProfileResponse, ShippingAddress } from '../../types/api.types';
import { catalogValue } from '../storefront-fixtures';
import { CatalogContext } from '../../contexts/catalog-context';
import { createMockAuth } from '../test-utils';

const ANA: ShippingAddress = { firstName: 'Ana', lastName: 'Novak', street: 'Main St 1', postalCode: '10115', city: 'Berlin', country: 'DE' };
const JANE: ShippingAddress = { firstName: 'Jane', lastName: 'Brown', street: '1 High St', postalCode: 'SW1A 1AA', city: 'London', country: 'GB' };
const savedProfile: ProfileResponse = { ...ANA, email: 'ana.novak@example.com' };
const emptyProfile: ProfileResponse = {
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana.novak@example.com',
  street: null,
  postalCode: null,
  city: null,
  country: null,
};

const storedCart = () => JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? '[]') as unknown;

function renderCheckout(profile: ProfileResponse) {
  vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
  return render(
    <AuthContext.Provider value={createMockAuth({}, ['CUSTOMER'])}>
      <NotificationProvider>
        <CatalogContext.Provider value={catalogValue()}>
          <CartProvider>
            <MemoryRouter initialEntries={['/checkout']}>
              <Routes>
                <Route path="/checkout" element={<Checkout />} />
                <Route path="/cart" element={<div>cart page</div>} />
                <Route path="/checkout/confirmation" element={<OrderConfirmation />} />
              </Routes>
            </MemoryRouter>
          </CartProvider>
        </CatalogContext.Provider>
      </NotificationProvider>
    </AuthContext.Provider>,
  );
}

async function fillAddress(address: ShippingAddress) {
  const fields: [string, string][] = [
    ['Shipping First Name', address.firstName],
    ['Shipping Last Name', address.lastName],
    ['Shipping Street', address.street],
    ['Shipping Postal Code', address.postalCode],
    ['Shipping City', address.city],
  ];
  await screen.findByLabelText('Shipping First Name'); // the form appears once the profile is read
  for (const [label, value] of fields) {
    await userEvent.clear(screen.getByLabelText(label));
    if (value) await userEvent.type(screen.getByLabelText(label), value);
  }
  await userEvent.selectOptions(screen.getByLabelText('Shipping Country'), address.country);
}

describe('placeCheckoutOrders', () => {
  it('places one order per line in cart order and keeps going after a rejection', async () => {
    const calls: string[] = [];
    const result = await placeCheckoutOrders(
      [
        { sku: 'DELL-XPS', quantity: 1 },
        { sku: 'LOGI-MX3S', quantity: 2 },
      ],
      ANA,
      'C1',
      (sku, _quantity, _address, coupon) => {
        calls.push(`${sku}:${coupon}`);
        return Promise.resolve(
          sku === 'DELL-XPS'
            ? { success: false, error: { message: 'invalid', fieldErrors: ['sku: Product does not exist for SKU: DELL-XPS'] } }
            : { success: true, data: { orderNumber: 'ORD-2' } },
        );
      },
    );
    expect(calls).toEqual(['DELL-XPS:C1', 'LOGI-MX3S:C1']);
    expect(result.placed).toEqual([{ sku: 'LOGI-MX3S', quantity: 2, orderNumber: 'ORD-2' }]);
    expect(result.rejected).toEqual([{ sku: 'DELL-XPS', quantity: 1, reason: 'Product does not exist for SKU: DELL-XPS' }]);
  });

  it("reports the backend's message when it names no field", () => {
    expect(reasonOf({ message: 'Orders cannot be placed right now' })).toBe('Orders cannot be placed right now');
  });

  it('validates every address field and the shipping country', () => {
    expect(validateAddress(ANA)).toEqual({});
    expect(validateAddress({ ...ANA, city: '   ', country: '' })).toEqual({ city: 'City is required', country: 'Country is required' });
    expect(validateAddress({ ...ANA, country: 'FR' })).toEqual({ country: 'Country must be US, DE or GB' });
    expect(formatAddress(ANA)).toBe('Ana Novak, Main St 1, 10115 Berlin, DE');
  });
});

describe('checkout page', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  afterEach(() => vi.restoreAllMocks());

  it('preselects the saved address and reviews the cart with the estimated tax', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 2 }, { sku: 'DELL-XPS', quantity: 1 }]));
    renderCheckout(savedProfile);
    expect(await screen.findByLabelText('Use Saved Address')).toBeChecked();
    expect(screen.getByText('Ana Novak, Main St 1, 10115 Berlin, DE')).toBeInTheDocument();
    expect(screen.getByLabelText('Review Line Total HP-15')).toHaveTextContent('$1,399.98');
    expect(screen.getByLabelText('Review Subtotal')).toHaveTextContent('$2,699.97');
    expect(screen.getByLabelText('Tax Country')).toHaveTextContent('DE');
    expect(screen.getByLabelText('Estimated Tax Rate')).toHaveTextContent('19%');

    await userEvent.click(screen.getByLabelText('Ship to a Different Address'));
    await fillAddress(JANE);
    expect(screen.getByLabelText('Tax Country')).toHaveTextContent('GB');
    expect(screen.getByLabelText('Estimated Tax Rate')).toHaveTextContent('20%');
  });

  it('places every line, empties the cart and confirms each order', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 2 }, { sku: 'DELL-XPS', quantity: 1 }]));
    const place = vi
      .spyOn(orderService, 'placeOrderTo')
      .mockResolvedValueOnce({ success: true, data: { orderNumber: 'ORD-1' } })
      .mockResolvedValueOnce({ success: true, data: { orderNumber: 'ORD-2' } });
    const save = vi.spyOn(profileService, 'updateProfile');
    renderCheckout(savedProfile);
    await userEvent.type(await screen.findByLabelText('Checkout Coupon Code'), 'SUMMER2025');
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));

    expect(await screen.findByText('Thank you for your order!')).toBeInTheDocument();
    expect(place.mock.calls).toEqual([
      ['HP-15', 2, ANA, 'SUMMER2025'],
      ['DELL-XPS', 1, ANA, 'SUMMER2025'],
    ]);
    expect(screen.getByRole('link', { name: 'Confirmed Order ORD-1' })).toHaveAttribute('href', '/order-details/ORD-1');
    expect(screen.getByRole('link', { name: 'Confirmed Order ORD-2' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View my orders' })).toHaveAttribute('href', '/order-history');
    expect(screen.getByRole('link', { name: 'Continue shopping' })).toHaveAttribute('href', '/');
    expect(storedCart()).toEqual([]);
    expect(save).not.toHaveBeenCalled();
  });

  it('keeps the rejected lines in the cart and shows why on the confirmation', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }, { sku: 'DELL-XPS', quantity: 1 }]));
    vi.spyOn(orderService, 'placeOrderTo')
      .mockResolvedValueOnce({ success: true, data: { orderNumber: 'ORD-1' } })
      .mockResolvedValueOnce({
        success: false,
        error: { message: 'invalid', fieldErrors: ['couponCode: Coupon code LIMITED2024 has exceeded its usage limit'] },
      });
    renderCheckout(savedProfile);
    await userEvent.type(await screen.findByLabelText('Checkout Coupon Code'), 'LIMITED2024');
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));

    expect(await screen.findByRole('link', { name: 'Confirmed Order ORD-1' })).toBeInTheDocument();
    expect(screen.getByLabelText('Rejection Reason DELL-XPS')).toHaveTextContent('Coupon code LIMITED2024 has exceeded its usage limit');
    expect(storedCart()).toEqual([{ sku: 'DELL-XPS', quantity: 1 }]);
  });

  it('stays on checkout with the cart unchanged when every line is rejected', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }, { sku: 'DELL-XPS', quantity: 1 }]));
    vi.spyOn(orderService, 'placeOrderTo').mockResolvedValue({
      success: false,
      error: { message: 'invalid', fieldErrors: ['couponCode: Coupon code INVALIDCOUPON does not exist'] },
    });
    renderCheckout(savedProfile);
    await userEvent.type(await screen.findByLabelText('Checkout Coupon Code'), 'INVALIDCOUPON');
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByText('HP-15: Coupon code INVALIDCOUPON does not exist')).toHaveClass('field-error');
    expect(within(alert).getByText('DELL-XPS: Coupon code INVALIDCOUPON does not exist')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Place Checkout Order' })).toBeInTheDocument();
    expect(storedCart()).toEqual([{ sku: 'HP-15', quantity: 1 }, { sku: 'DELL-XPS', quantity: 1 }]);
  });

  it('opens the address form on a first order and saves it as the default', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }]));
    const place = vi.spyOn(orderService, 'placeOrderTo').mockResolvedValue({ success: true, data: { orderNumber: 'ORD-9' } });
    const save = vi.spyOn(profileService, 'updateProfile').mockResolvedValue({ success: true, data: savedProfile });
    renderCheckout(emptyProfile);
    expect(await screen.findByLabelText('Shipping First Name')).toHaveValue('Ana');
    expect(screen.getByLabelText('Shipping Last Name')).toHaveValue('Novak');
    expect(screen.getByLabelText('Save as Default Address')).toBeChecked();

    await fillAddress(ANA);
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));
    expect(await screen.findByText('Thank you for your order!')).toBeInTheDocument();
    expect(place).toHaveBeenCalledWith('HP-15', 1, ANA, undefined);
    expect(save).toHaveBeenCalledWith(ANA);
  });

  it('does not save the address when "Save as default" is unticked', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }]));
    vi.spyOn(orderService, 'placeOrderTo').mockResolvedValue({ success: true, data: { orderNumber: 'ORD-9' } });
    const save = vi.spyOn(profileService, 'updateProfile');
    renderCheckout(emptyProfile);
    await fillAddress(ANA);
    await userEvent.click(screen.getByLabelText('Save as Default Address'));
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));
    expect(await screen.findByText('Thank you for your order!')).toBeInTheDocument();
    expect(save).not.toHaveBeenCalled();
  });

  it('rejects an incomplete address without placing anything', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }]));
    const place = vi.spyOn(orderService, 'placeOrderTo');
    renderCheckout(emptyProfile);
    await fillAddress({ ...ANA, street: '' });
    await userEvent.click(screen.getByRole('button', { name: 'Place Checkout Order' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Street is required');
    expect(place).not.toHaveBeenCalled();
    expect(storedCart()).toEqual([{ sku: 'HP-15', quantity: 1 }]);
  });

  it('sends an empty cart to the cart page', async () => {
    renderCheckout(savedProfile);
    expect(await screen.findByText('cart page')).toBeInTheDocument();
  });
});
