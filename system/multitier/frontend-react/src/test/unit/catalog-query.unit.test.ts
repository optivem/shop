import { describe, expect, it } from 'vitest';
import {
  applyCatalogQuery,
  brandsOf,
  categoriesOf,
  parseCatalogQuery,
  toSearchParams,
  type CatalogQuery,
} from '../../features/catalog/catalog-query';
import { CATALOG } from '../storefront-fixtures';

const query = (overrides: Partial<CatalogQuery> = {}): CatalogQuery => ({ q: '', category: '', brands: [], sort: 'featured', ...overrides });
const skus = (q: CatalogQuery) => applyCatalogQuery(CATALOG, q).map((p) => p.sku);

describe('catalog query (UC-006)', () => {
  it('lists every product in the ERP order by default', () => {
    expect(skus(query())).toEqual(['HP-15', 'DELL-XPS', 'LENOVO-T14', 'DELL-U2723QE', 'LG-27UL500', 'LOGI-MX3S']);
  });

  it('offers the categories and brands of the catalog, in first-seen order', () => {
    expect(categoriesOf(CATALOG)).toEqual(['Laptops', 'Monitors', 'Accessories']);
    expect(brandsOf(CATALOG)).toEqual(['HP', 'Dell', 'Lenovo', 'LG', 'Logitech']);
  });

  it('filters by category, brands and search together', () => {
    expect(skus(query({ category: 'Monitors' }))).toEqual(['DELL-U2723QE', 'LG-27UL500']);
    expect(skus(query({ brands: ['Dell', 'LG'] }))).toEqual(['DELL-XPS', 'DELL-U2723QE', 'LG-27UL500']);
    expect(skus(query({ category: 'Laptops', brands: ['Dell'] }))).toEqual(['DELL-XPS']);
  });

  it.each([
    ['thinkpad', ['LENOVO-T14']],
    ['LOGITECH', ['LOGI-MX3S']],
    ['wireless', ['LOGI-MX3S']],
    ['4k', ['DELL-U2723QE', 'LG-27UL500']],
    ['printer', []],
  ])('searches title, brand and description ignoring case: %s', (q, expected) => {
    expect(skus(query({ q }))).toEqual(expected);
  });

  it.each([
    ['price-asc', ['LOGI-MX3S', 'LG-27UL500', 'DELL-U2723QE', 'HP-15', 'LENOVO-T14', 'DELL-XPS']],
    ['price-desc', ['DELL-XPS', 'LENOVO-T14', 'HP-15', 'DELL-U2723QE', 'LG-27UL500', 'LOGI-MX3S']],
    ['name', ['DELL-U2723QE', 'DELL-XPS', 'HP-15', 'LENOVO-T14', 'LG-27UL500', 'LOGI-MX3S']],
  ] as const)('sorts by %s', (sort, expected) => {
    expect(skus(query({ sort }))).toEqual(expected);
  });

  it('sorts only the filtered products', () => {
    expect(skus(query({ category: 'Laptops', sort: 'price-desc' }))).toEqual(['DELL-XPS', 'LENOVO-T14', 'HP-15']);
  });

  it('round-trips through the page address and leaves defaults out', () => {
    const q = query({ q: 'dell', category: 'Laptops', brands: ['Dell', 'HP'], sort: 'price-desc' });
    expect(parseCatalogQuery(toSearchParams(q))).toEqual(q);
    expect(toSearchParams(query()).toString()).toBe('');
    expect(parseCatalogQuery(new URLSearchParams('sort=bogus')).sort).toBe('featured');
  });
});
