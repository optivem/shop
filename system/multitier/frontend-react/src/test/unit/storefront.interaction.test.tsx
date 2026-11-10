// Interaction tests for the storefront pages: rendered with the providers they use, driven through userEvent.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { AuthContext, type AuthContextProps } from 'react-oidc-context';
import type { ReactNode } from 'react';
import { CartProvider } from '../../contexts/CartContext';
import { CART_STORAGE_KEY } from '../../contexts/cart-storage';
import { NotificationProvider } from '../../contexts/NotificationContext';
import { SessionCheckedContext } from '../../auth/session-check';
import { Cart } from '../../pages/Cart';
import { Catalog } from '../../pages/Catalog';
import { ProductDetail } from '../../pages/ProductDetail';
import { productService } from '../../services/product-service';
import { catalogValue, CATALOG } from '../storefront-fixtures';
import { CatalogContext } from '../../contexts/catalog-context';
import { createMockAuth } from '../test-utils';
import type { CatalogContextType } from '../../contexts/catalog-context';

function LocationProbe() {
  const location = useLocation();
  return <div data-testid="location">{`${location.pathname}${location.search}`}</div>;
}

function renderStore(
  ui: ReactNode,
  {
    path = '/',
    entry = '/',
    catalog = catalogValue(),
    auth = createMockAuth({ isAuthenticated: false, user: null }),
    sessionChecked = true,
  }: { path?: string; entry?: string; catalog?: CatalogContextType; auth?: AuthContextProps; sessionChecked?: boolean } = {},
) {
  return render(
    <AuthContext.Provider value={auth}>
      <SessionCheckedContext.Provider value={sessionChecked}>
        <NotificationProvider>
          <CatalogContext.Provider value={catalog}>
            <CartProvider>
              <MemoryRouter initialEntries={[entry]}>
                <Routes>
                  <Route path={path} element={ui} />
                </Routes>
                <LocationProbe />
              </MemoryRouter>
            </CartProvider>
          </CatalogContext.Provider>
        </NotificationProvider>
      </SessionCheckedContext.Provider>
    </AuthContext.Provider>,
  );
}

const storeCart = (lines: { sku: string; quantity: number }[]) => localStorage.setItem(CART_STORAGE_KEY, JSON.stringify(lines));
const card = (sku: string) => screen.getByRole('article', { name: `Product ${sku}` });
const shownSkus = () => screen.queryAllByRole('article').map((article) => article.getAttribute('aria-label')?.replace('Product ', ''));

describe('header', () => {
  beforeEach(() => localStorage.clear());

  it('offers an anonymous visitor "Sign In" back to the current page, and hides the signed-in links', () => {
    renderStore(<Catalog />, { entry: '/?category=Laptops' });
    expect(screen.getByRole('link', { name: 'Sign In' })).toHaveAttribute('href', '/sign-in?returnUrl=%2F%3Fcategory%3DLaptops');
    expect(document.querySelector("a[href='/order-history']")).toBeNull();
  });

  it('shows neither "Sign In" nor the signed-in links while the session is being checked', () => {
    renderStore(<Catalog />, { sessionChecked: false });
    expect(screen.queryByRole('link', { name: 'Sign In' })).not.toBeInTheDocument();
    expect(document.querySelector("a[href='/order-history']")).toBeNull();
  });

  it('shows the account links to a signed-in customer on the catalog', () => {
    renderStore(<Catalog />, { auth: createMockAuth({}, ['CUSTOMER']) });
    expect(document.querySelector("a[href='/new-order']")).not.toBeNull();
    expect(document.querySelector("a[href='/order-history']")).not.toBeNull();
    expect(document.querySelector("a[href='/admin-coupons']")).toBeNull();
    expect(screen.queryByRole('link', { name: 'Sign In' })).not.toBeInTheDocument();
  });

  it('links each catalog category and hides the badge for an empty cart', () => {
    renderStore(<Catalog />);
    const nav = screen.getByRole('navigation', { name: 'Store navigation' });
    expect(within(nav).getByRole('link', { name: 'Monitors' })).toHaveAttribute('href', '/?category=Monitors');
    expect(screen.queryByLabelText('Cart Item Count')).not.toBeInTheDocument();
  });

  it('searches from the header', async () => {
    renderStore(<Catalog />);
    await userEvent.type(screen.getByLabelText('Search Products'), 'thinkpad');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));
    expect(screen.getByTestId('location')).toHaveTextContent('/?q=thinkpad');
    expect(shownSkus()).toEqual(['LENOVO-T14']);
  });
});

describe('catalog page (UC-006)', () => {
  beforeEach(() => localStorage.clear());

  it('shows each product with title, brand and price, and the count', () => {
    renderStore(<Catalog />);
    expect(screen.getByLabelText('Product Count')).toHaveTextContent('6 products');
    const dell = within(card('DELL-XPS'));
    expect(dell.getByLabelText('Product Title')).toHaveTextContent('Dell XPS 13');
    expect(dell.getByLabelText('Product Brand')).toHaveTextContent('Dell');
    expect(dell.getByLabelText('Display Price')).toHaveTextContent('$1,299.99');
    expect(dell.getByLabelText('Product Title')).toHaveAttribute('href', '/products/DELL-XPS');
  });

  it('keeps the filters and sort in the page address', async () => {
    renderStore(<Catalog />);
    await userEvent.click(screen.getByLabelText('Laptops'));
    await userEvent.selectOptions(screen.getByLabelText('Sort'), 'price-desc');
    expect(screen.getByTestId('location')).toHaveTextContent('/?category=Laptops&sort=price-desc');
    expect(shownSkus()).toEqual(['DELL-XPS', 'LENOVO-T14', 'HP-15']);
  });

  it('opens a bookmarked filtered view', () => {
    renderStore(<Catalog />, { entry: '/?brand=Dell&brand=LG' });
    expect(shownSkus()).toEqual(['DELL-XPS', 'DELL-U2723QE', 'LG-27UL500']);
    expect(screen.getByLabelText('Dell')).toBeChecked();
    expect(screen.getByLabelText('All')).toBeChecked();
  });

  it('tells the visitor when nothing matches and offers to clear the filters', async () => {
    renderStore(<Catalog />, { entry: '/?q=printer' });
    expect(screen.getByText('No products match your search.')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('link', { name: 'Clear Filters' }));
    expect(shownSkus()).toHaveLength(6);
  });

  it('tells the visitor when the ERP is unavailable', () => {
    renderStore(<Catalog />, { catalog: catalogValue({ products: [], error: 'Service Unavailable' }) });
    expect(screen.getByText('Products are temporarily unavailable.')).toHaveClass('alert-danger');
    expect(screen.queryAllByRole('article')).toHaveLength(0);
  });

  it('adds quantity 1 to the cart and updates the badge', async () => {
    renderStore(<Catalog />);
    await userEvent.click(screen.getByRole('button', { name: 'Add LOGI-MX3S to Cart' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add LOGI-MX3S to Cart' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Added to cart');
    expect(screen.getByLabelText('Cart Item Count')).toHaveTextContent('2');
    expect(JSON.parse(localStorage.getItem(CART_STORAGE_KEY) ?? '[]')).toEqual([{ sku: 'LOGI-MX3S', quantity: 2 }]);
  });
});

describe('product page (UC-007)', () => {
  beforeEach(() => localStorage.clear());
  afterEach(() => vi.restoreAllMocks());

  const viewProduct = (sku: string) => renderStore(<ProductDetail />, { path: '/products/:sku', entry: `/products/${sku}` });

  it('shows the product details and breadcrumbs', async () => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: true, data: CATALOG[5]! });
    viewProduct('LOGI-MX3S');
    expect(await screen.findByLabelText('Product Title')).toHaveTextContent('Logitech MX Master 3S');
    expect(screen.getByLabelText('Product Category')).toHaveTextContent('Accessories');
    expect(screen.getByLabelText('Product SKU')).toHaveTextContent('LOGI-MX3S');
    expect(screen.getByLabelText('Display Price')).toHaveTextContent('$99.99');
    const crumbs = within(screen.getByRole('navigation', { name: 'breadcrumb' })).getAllByRole('listitem');
    expect(crumbs.map((c) => c.textContent)).toEqual(['Home', 'Accessories', 'Logitech MX Master 3S']);
    expect(screen.getByLabelText('Product Quantity')).toHaveValue('1');
  });

  it('keeps the stepper between 1 and 99 and adds the chosen quantity', async () => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: true, data: CATALOG[0]! });
    viewProduct('HP-15');
    const quantity = await screen.findByLabelText('Product Quantity');
    await userEvent.click(screen.getByRole('button', { name: 'Decrease Quantity' }));
    expect(quantity).toHaveValue('1');
    await userEvent.click(screen.getByRole('button', { name: 'Increase Quantity' }));
    await userEvent.click(screen.getByRole('button', { name: 'Increase Quantity' }));
    await userEvent.click(screen.getByRole('button', { name: 'Add to Cart' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Added to cart');
    expect(screen.getByLabelText('Cart Item Count')).toHaveTextContent('3');
    expect(screen.getByTestId('location')).toHaveTextContent('/products/HP-15');
    await userEvent.clear(quantity);
    await userEvent.type(quantity, '99');
    await userEvent.click(screen.getByRole('button', { name: 'Increase Quantity' }));
    expect(quantity).toHaveValue('99');
  });

  it.each(['0', '-1', '1.5', 'abc', '100'])('does not add the quantity %s', async (value) => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: true, data: CATALOG[0]! });
    viewProduct('HP-15');
    const quantity = await screen.findByLabelText('Product Quantity');
    await userEvent.clear(quantity);
    await userEvent.type(quantity, value);
    await userEvent.click(screen.getByRole('button', { name: 'Add to Cart' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Quantity must be a whole number from 1 to 99.');
    expect(localStorage.getItem(CART_STORAGE_KEY)).toBe('[]');
  });

  it('shows "Product not found" with a link back to the catalog', async () => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: false, error: { message: 'Not found', status: 404 } });
    viewProduct('NO-SUCH-SKU');
    expect(await screen.findByRole('heading', { name: 'Product not found' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Back to Catalog' })).toHaveAttribute('href', '/');
  });

  it('tells the visitor when the ERP is unavailable and offers no add to cart', async () => {
    vi.spyOn(productService, 'viewProduct').mockResolvedValue({ success: false, error: { message: 'down', status: 503 } });
    viewProduct('HP-15');
    expect(await screen.findByText('Products are temporarily unavailable.')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Add to Cart' })).not.toBeInTheDocument();
  });
});

describe('cart page (UC-008)', () => {
  beforeEach(() => localStorage.clear());

  it('shows each line at current catalog prices, with the subtotal', () => {
    storeCart([
      { sku: 'HP-15', quantity: 2 },
      { sku: 'DELL-XPS', quantity: 1 },
    ]);
    const catalog = catalogValue();
    renderStore(<Cart />, { path: '/cart', entry: '/cart', catalog });
    expect(catalog.reload).toHaveBeenCalled();
    expect(screen.getByLabelText('Cart Line Count')).toHaveTextContent('(2 items)');
    const hp = within(screen.getByRole('row', { name: 'Cart Line HP-15' }));
    expect(hp.getByLabelText('Unit Price HP-15')).toHaveTextContent('$699.99');
    expect(hp.getByLabelText('Line Total HP-15')).toHaveTextContent('$1,399.98');
    expect(screen.getByLabelText('Cart Subtotal')).toHaveTextContent('$2,699.97');
    expect(screen.getByText('Tax and discounts are calculated at checkout.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Proceed to Checkout' })).toHaveAttribute('href', '/checkout');
  });

  it('updates the line total, subtotal and badge when the quantity changes', async () => {
    storeCart([
      { sku: 'HP-15', quantity: 2 },
      { sku: 'DELL-XPS', quantity: 1 },
    ]);
    renderStore(<Cart />, { path: '/cart', entry: '/cart' });
    const quantity = screen.getByLabelText('Quantity for HP-15');
    await userEvent.clear(quantity);
    await userEvent.type(quantity, '3{Enter}');
    expect(screen.getByLabelText('Line Total HP-15')).toHaveTextContent('$2,099.97');
    expect(screen.getByLabelText('Cart Subtotal')).toHaveTextContent('$3,399.96');
    expect(screen.getByLabelText('Cart Item Count')).toHaveTextContent('4');
    await userEvent.click(screen.getByRole('button', { name: 'Increase Quantity for HP-15' }));
    expect(quantity).toHaveValue('4');
  });

  it('keeps the previous quantity when the new one is not valid', async () => {
    storeCart([{ sku: 'HP-15', quantity: 2 }]);
    renderStore(<Cart />, { path: '/cart', entry: '/cart' });
    const quantity = screen.getByLabelText('Quantity for HP-15');
    await userEvent.clear(quantity);
    await userEvent.type(quantity, '100{Enter}');
    expect(screen.getByRole('alert')).toHaveTextContent('Quantity must be a whole number from 1 to 99.');
    expect(quantity).toHaveValue('2');
  });

  it('removes a line, and says the cart is empty after the last one', async () => {
    storeCart([
      { sku: 'HP-15', quantity: 2 },
      { sku: 'DELL-XPS', quantity: 1 },
    ]);
    renderStore(<Cart />, { path: '/cart', entry: '/cart' });
    await userEvent.click(screen.getByRole('button', { name: 'Remove HP-15' }));
    expect(screen.getByLabelText('Cart Subtotal')).toHaveTextContent('$1,299.99');
    expect(screen.getByLabelText('Cart Item Count')).toHaveTextContent('1');
    await userEvent.click(screen.getByRole('button', { name: 'Remove DELL-XPS' }));
    expect(screen.getByText('Your cart is empty.')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Start shopping' })).toHaveAttribute('href', '/');
    expect(screen.queryByLabelText('Cart Item Count')).not.toBeInTheDocument();
  });

  it('keeps the cart when the ERP is unavailable', () => {
    storeCart([{ sku: 'HP-15', quantity: 2 }]);
    renderStore(<Cart />, { path: '/cart', entry: '/cart', catalog: catalogValue({ products: [], error: 'down' }) });
    expect(screen.getByText('Products are temporarily unavailable.')).toBeInTheDocument();
    expect(screen.getByLabelText('Cart Item Count')).toHaveTextContent('2');
  });
});
