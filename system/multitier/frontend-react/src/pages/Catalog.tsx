import { useState } from 'react';
import { SlidersHorizontal } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { LoadingSpinner, StoreLayout } from '../components';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { CatalogFilters } from '../features/catalog/CatalogFilters';
import { PromoTiles } from '../features/catalog/PromoTiles';
import { ProductCard } from '../features/catalog/ProductCard';
import {
  applyCatalogQuery,
  brandsOf,
  categoriesOf,
  parseCatalogQuery,
  SORT_OPTIONS,
  toSearchParams,
  type CatalogQuery,
  type SortOrder,
} from '../features/catalog/catalog-query';
import { ADDED_TO_CART, PRODUCTS_UNAVAILABLE } from '../utils/messages';

/** The home page: browse, search, filter and sort the catalog (UC-006); add a product with quantity 1. */
export function Catalog() {
  const { products, isLoading, error } = useCatalog();
  const { add } = useCart();
  const { setSuccess } = useNotificationContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  // The query lives in the page address (BR-7), so a filtered view can be bookmarked.
  const query = parseCatalogQuery(searchParams);
  const shown = applyCatalogQuery(products, query);
  const setQuery = (next: CatalogQuery) => setSearchParams(toSearchParams(next));

  const addToCart = (sku: string) => {
    add(sku, 1);
    setSuccess(ADDED_TO_CART);
  };

  const renderResults = () => {
    if (isLoading && products.length === 0) return <LoadingSpinner message="Loading products..." />;
    if (error) {
      return (
        <div className="alert alert-danger" role="alert">
          {PRODUCTS_UNAVAILABLE}
        </div>
      );
    }
    if (shown.length === 0) {
      return (
        <div className="market-empty text-center py-5 px-3">
          <p className="lead">No products match your search.</p>
          <Link to="/" className="btn btn-outline-primary" aria-label="Clear Filters">
            Clear filters
          </Link>
        </div>
      );
    }
    return (
      <div className="product-grid" aria-label="Product List">
        {shown.map((product) => (
          <ProductCard key={product.sku} product={product} onAddToCart={addToCart} />
        ))}
      </div>
    );
  };

  return (
    <StoreLayout>
      <PromoTiles />
      <div className="catalog-layout">
        <aside className="catalog-sidebar">
          <button
            type="button"
            className="btn btn-outline-secondary w-100 d-lg-none"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            <SlidersHorizontal size={16} aria-hidden="true" className="me-2" />
            Filters
          </button>
          <div className={filtersOpen ? 'mt-3 mt-lg-0' : 'd-none d-lg-block'}>
            <CatalogFilters
              categories={categoriesOf(products)}
              brands={brandsOf(products)}
              query={query}
              onChange={setQuery}
            />
          </div>
        </aside>
        <section className="catalog-results" aria-label="Catalog">
          <div className="catalog-toolbar">
            <p className="mb-0">
              <strong aria-label="Product Count">
                {error ? 0 : shown.length} {shown.length === 1 && !error ? 'product' : 'products'}
              </strong>{' '}
              <span className="text-body-secondary">in {query.category || 'All products'}</span>
            </p>
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="catalog-sort" className="form-label mb-0 text-body-secondary">
                Sort by
              </label>
              <select
                id="catalog-sort"
                className="form-select w-auto"
                aria-label="Sort"
                value={query.sort}
                onChange={(e) => setQuery({ ...query, sort: e.target.value as SortOrder })}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          {renderResults()}
        </section>
      </div>
    </StoreLayout>
  );
}
