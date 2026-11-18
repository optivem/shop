// Accessibility screen tests (FT Step 3): each main screen, rendered with data, has no
// serious or critical axe violations. Runs in the default `npm test` with the API mocked
// at the service boundary, like the interaction tests next to it.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import type { ReactNode } from 'react';
import { SessionCheckedContext } from '../../auth/session-check';
import { CartProvider } from '../../contexts/CartContext';
import { CART_STORAGE_KEY } from '../../contexts/cart-storage';
import { CatalogContext } from '../../contexts/catalog-context';
import { NotificationProvider } from '../../contexts/NotificationContext';
import { Account } from '../../pages/Account';
import { Cart } from '../../pages/Cart';
import { Catalog } from '../../pages/Catalog';
import { Checkout } from '../../pages/Checkout';
import { OrderDetails } from '../../pages/OrderDetails';
import { OrderHistory } from '../../pages/OrderHistory';
import { ProductDetail } from '../../pages/ProductDetail';
import { SignIn } from '../../pages/SignIn';
import { orderService } from '../../services/order-service';
import { productService } from '../../services/product-service';
import { profileService } from '../../services/profile-service';
import type { ProfileResponse } from '../../types/api.types';
import { seriousA11yViolations } from '../axe';
import { catalogValue, CATALOG } from '../storefront-fixtures';
import { createMockAuth } from '../test-utils';

const customer = () => createMockAuth({}, ['CUSTOMER']);

function renderScreen(ui: ReactNode, { path, entry = path, auth = customer() }: { path: string; entry?: string; auth?: AuthContextProps }) {
  return render(
    <AuthContext.Provider value={auth}>
      <SessionCheckedContext.Provider value={true}>
        <NotificationProvider>
          <CatalogContext.Provider value={catalogValue()}>
            <CartProvider>
              <MemoryRouter initialEntries={[entry]}>
                <Routes>
                  <Route path={path} element={ui} />
                </Routes>
              </MemoryRouter>
            </CartProvider>
          </CatalogContext.Provider>
        </NotificationProvider>
      </SessionCheckedContext.Provider>
    </AuthContext.Provider>,
  );
}

const profile: ProfileResponse = {
  firstName: 'Ana',
  lastName: 'Novak',
  email: 'ana.novak@example.com',
  street: 'Main St 1',
  postalCode: '10115',
  city: 'Berlin',
  country: 'DE',
};

const newCustomer: ProfileResponse = { ...profile, street: null, postalCode: null, city: null, country: null };

describe('accessibility (axe, serious and critical)', () => {
  beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });
  afterEach(() => vi.restoreAllMocks());

  it('catalog', async () => {
    const { container } = renderScreen(<Catalog />, { path: '/', auth: createMockAuth({ isAuthenticated: false, user: null }) });
    expect(screen.getAllByRole('article')).toHaveLength(CATALOG.length);
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('product page', async () => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: true, data: CATALOG[0]! });
    const { container } = renderScreen(<ProductDetail />, { path: '/products/:sku', entry: '/products/HP-15' });
    await screen.findByLabelText('Product Title');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('cart', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 2 }, { sku: 'LOGI-MX3S', quantity: 1 }]));
    const { container } = renderScreen(<Cart />, { path: '/cart' });
    await screen.findByText('Logitech MX Master 3S');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('checkout with a saved address', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }]));
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
    const { container } = renderScreen(<Checkout />, { path: '/checkout' });
    await screen.findByLabelText('Use Saved Address');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('checkout with the address form', async () => {
    localStorage.setItem(CART_STORAGE_KEY, JSON.stringify([{ sku: 'HP-15', quantity: 1 }]));
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: newCustomer });
    const { container } = renderScreen(<Checkout />, { path: '/checkout' });
    await screen.findByLabelText('Shipping First Name');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('order history', async () => {
    vi.spyOn(orderService, 'browseOrderHistory').mockResolvedValue({
      success: true,
      data: {
        orders: [
          {
            orderNumber: 'ORD-1001',
            orderTimestamp: '2026-11-01T09:00:00Z',
            country: 'US',
            sku: 'HP-15',
            quantity: 1,
            totalPrice: '699.99',
            appliedCouponCode: null,
            status: 'PLACED',
            customer: 'customer1',
          },
        ],
      },
    });
    const { container } = renderScreen(<OrderHistory />, { path: '/order-history' });
    await screen.findByText('ORD-1001');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('order details', async () => {
    vi.spyOn(orderService, 'getOrder').mockResolvedValue({
      success: true,
      data: {
        orderNumber: 'ORD-1001',
        orderTimestamp: '2026-11-01T09:00:00Z',
        country: 'DE',
        sku: 'HP-15',
        quantity: 1,
        unitPrice: '699.99',
        basePrice: '699.99',
        discountRate: '0',
        discountAmount: '0.00',
        subtotalPrice: '699.99',
        taxRate: '0.19',
        taxAmount: '133.00',
        totalPrice: '832.99',
        appliedCouponCode: null,
        status: 'PLACED',
        shippingAddress: { firstName: 'Ana', lastName: 'Novak', street: 'Main St 1', postalCode: '10115', city: 'Berlin', country: 'DE' },
      },
    });
    const { container } = renderScreen(<OrderDetails />, { path: '/order-details/:orderNumber', entry: '/order-details/ORD-1001' });
    await screen.findByLabelText('Display Order Number');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('sign-in redirect', async () => {
    const auth = createMockAuth({ isAuthenticated: false, user: null, signinRedirect: vi.fn().mockResolvedValue(undefined) });
    const { container } = renderScreen(<SignIn />, { path: '/sign-in', entry: '/sign-in?returnUrl=%2Fcheckout', auth });
    await screen.findByRole('status');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });

  it('profile', async () => {
    vi.spyOn(profileService, 'viewProfile').mockResolvedValue({ success: true, data: profile });
    const { container } = renderScreen(<Account />, { path: '/account' });
    await screen.findByLabelText('First Name');
    expect(await seriousA11yViolations(container)).toEqual([]);
  });
});
