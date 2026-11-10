// Catalog filtering and sorting (UC-006 BR-2..BR-7). The catalog is small and the ERP has no query API,
// so filtering happens here over the full list, and the query lives in the page address.

import type { Product } from '../../types/api.types';

export const SORT_OPTIONS = [
  { value: 'featured', label: 'Featured' },
  { value: 'price-asc', label: 'Price: low to high' },
  { value: 'price-desc', label: 'Price: high to low' },
  { value: 'name', label: 'Name: A to Z' },
] as const;

export type SortOrder = (typeof SORT_OPTIONS)[number]['value'];

export interface CatalogQuery {
  q: string;
  category: string; // '' means All
  brands: string[];
  sort: SortOrder;
}

const isSortOrder = (value: string | null): value is SortOrder => SORT_OPTIONS.some((option) => option.value === value);

export function parseCatalogQuery(params: URLSearchParams): CatalogQuery {
  const sort = params.get('sort');
  return {
    q: params.get('q') ?? '',
    category: params.get('category') ?? '',
    brands: params.getAll('brand').filter((brand) => brand !== ''),
    sort: isSortOrder(sort) ? sort : 'featured',
  };
}

/** The page address for a query; defaults are left out so the plain catalog is just "/". */
export function toSearchParams(query: CatalogQuery): URLSearchParams {
  const params = new URLSearchParams();
  if (query.q.trim()) params.set('q', query.q.trim());
  if (query.category) params.set('category', query.category);
  query.brands.forEach((brand) => params.append('brand', brand));
  if (query.sort !== 'featured') params.set('sort', query.sort);
  return params;
}

/** Distinct values in first-seen (ERP) order. */
function distinct(values: string[]): string[] {
  return [...new Set(values)];
}

export const categoriesOf = (products: Product[]) => distinct(products.map((p) => p.category));
export const brandsOf = (products: Product[]) => distinct(products.map((p) => p.brand));

function matches(product: Product, query: CatalogQuery): boolean {
  const term = query.q.trim().toLowerCase();
  const matchesTerm =
    term === '' ||
    [product.title, product.brand, product.description].some((text) => text.toLowerCase().includes(term));
  const matchesCategory = query.category === '' || product.category === query.category;
  const matchesBrand = query.brands.length === 0 || query.brands.includes(product.brand);
  return matchesTerm && matchesCategory && matchesBrand;
}

function compare(sort: SortOrder): ((a: Product, b: Product) => number) | undefined {
  switch (sort) {
    case 'price-asc':
      return (a, b) => Number(a.price) - Number(b.price);
    case 'price-desc':
      return (a, b) => Number(b.price) - Number(a.price);
    case 'name':
      return (a, b) => a.title.localeCompare(b.title, 'en');
    default:
      return undefined;
  }
}

/** The products matching every filter, in the chosen order (Featured keeps the ERP order). */
export function applyCatalogQuery(products: Product[], query: CatalogQuery): Product[] {
  const filtered = products.filter((product) => matches(product, query));
  const comparator = compare(query.sort);
  return comparator ? [...filtered].sort(comparator) : filtered;
}
