import { applyCatalogQuery, catalogPath, categoriesOf, parseCatalogQuery } from '@/lib/storefront/catalog-query';
import { parseCart, serializeCart } from '@/lib/storefront/cart-cookie';
import { priceCart } from '@/lib/storefront/cart-lines';
import { placeCheckoutOrders } from '@/lib/storefront/place-checkout-orders';
import { parseQuantity } from '@/lib/storefront/quantity';
import { savedAddressOf, validateAddress } from '@/lib/storefront/shipping';
import type { Product } from '@/lib/storefront/types';

const product = (sku: string, title: string, brand: string, category: string, price: string): Product => ({
  sku,
  title,
  brand,
  category,
  price,
  description: `${title} description`,
});

const CATALOG = [
  product('HP-15', 'HP Laptop 15', 'HP', 'Laptops', '699.99'),
  product('DELL-XPS', 'Dell XPS 13', 'Dell', 'Laptops', '1299.99'),
  product('LG-27UL500', 'LG 27-inch 4K UHD Monitor', 'LG', 'Monitors', '299.99'),
];

describe('catalog query', () => {
  it('reads the filters from the query string, ignoring an unknown sort', () => {
    expect(parseCatalogQuery({ q: 'dell', category: 'Laptops', brand: ['Dell', 'HP'], sort: 'bogus' })).toEqual({
      q: 'dell',
      category: 'Laptops',
      brands: ['Dell', 'HP'],
      sort: 'featured',
    });
  });

  it('leaves the defaults out of the page address', () => {
    expect(catalogPath({ q: '', category: '', brands: [], sort: 'featured' })).toBe('/');
    expect(catalogPath({ q: ' xps ', category: 'Laptops', brands: ['Dell'], sort: 'price-asc' })).toBe(
      '/?q=xps&category=Laptops&brand=Dell&sort=price-asc',
    );
  });

  it('filters by every criterion and sorts only the matches', () => {
    const shown = applyCatalogQuery(CATALOG, { q: 'DESCRIPTION', category: 'Laptops', brands: [], sort: 'price-desc' });
    expect(shown.map((p) => p.sku)).toEqual(['DELL-XPS', 'HP-15']);
  });

  it('offers the categories in ERP order', () => {
    expect(categoriesOf(CATALOG)).toEqual(['Laptops', 'Monitors']);
  });
});

describe('cart cookie', () => {
  it('round-trips the cart', () => {
    const lines = [{ sku: 'HP-15', quantity: 2 }, { sku: 'DELL-XPS', quantity: 1 }];
    expect(parseCart(serializeCart(lines))).toEqual(lines);
  });

  it('treats a malformed cookie as an empty cart and caps quantities', () => {
    expect(parseCart('not json')).toEqual([]);
    expect(parseCart(undefined)).toEqual([]);
    expect(parseCart(JSON.stringify([{ sku: 'HP-15', quantity: 500 }, { sku: 'HP-15', quantity: 1 }, { sku: 1 }]))).toEqual([
      { sku: 'HP-15', quantity: 99 },
    ]);
  });
});

describe('cart pricing', () => {
  it('prices each line from the catalog in cents', () => {
    const { lines, subtotalCents } = priceCart([{ sku: 'HP-15', quantity: 3 }, { sku: 'GONE', quantity: 1 }], CATALOG);
    expect(lines.map((l) => l.lineTotalCents)).toEqual([209997, 0]);
    expect(subtotalCents).toBe(209997);
  });
});

describe('quantity', () => {
  it.each([['1', 1], ['99', 99], ['0', undefined], ['100', undefined], ['1.5', undefined], ['abc', undefined]])(
    '%s parses to %s',
    (value, expected) => {
      expect(parseQuantity(value)).toBe(expected);
    },
  );
});

describe('shipping address', () => {
  const complete = { firstName: 'Ana', lastName: 'Novak', street: 'Main St 1', postalCode: '10115', city: 'Berlin', country: 'DE' };

  it('requires every field and a supported country', () => {
    expect(validateAddress({ ...complete, city: '  ', country: 'FR' })).toEqual({
      city: 'City is required',
      country: 'Country must be US, DE or GB',
    });
  });

  it('has no saved address until the profile is complete', () => {
    expect(savedAddressOf({ ...complete, street: null })).toBeUndefined();
    expect(savedAddressOf(complete)).toEqual(complete);
  });
});

describe('checkout', () => {
  it('places every line in cart order and keeps the rejected ones with their reason', async () => {
    const result = await placeCheckoutOrders(
      [{ sku: 'HP-15', quantity: 1 }, { sku: 'BAD', quantity: 2 }],
      { firstName: 'A', lastName: 'B', street: 'S', postalCode: 'P', city: 'C', country: 'US' },
      undefined,
      (sku) =>
        Promise.resolve(
          sku === 'BAD'
            ? { success: false, message: 'The request contains one or more validation errors', fieldErrors: ['sku: Product does not exist'] }
            : { success: true, orderNumber: 'ORD-1' },
        ),
    );
    expect(result).toEqual({
      placed: [{ sku: 'HP-15', quantity: 1, orderNumber: 'ORD-1' }],
      rejected: [{ sku: 'BAD', quantity: 2, reason: 'Product does not exist' }],
    });
  });
});
