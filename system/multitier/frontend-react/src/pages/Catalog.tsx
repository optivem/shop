import { useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { LoadingSpinner, StoreLayout } from '../components';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { CatalogFilters } from '../features/catalog/CatalogFilters';
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
        <div className="text-center py-5">
          <p className="lead">No products match your search.</p>
          <Link to="/" className="btn btn-outline-primary" aria-label="Clear Filters">
            Clear filters
          </Link>
        </div>
      );
    }
    return (
      <div className="row row-cols-1 row-cols-sm-2 row-cols-lg-3 row-cols-xl-4 g-4" aria-label="Product List">
        {shown.map((product) => (
          <div className="col" key={product.sku}>
            <ProductCard product={product} onAddToCart={addToCart} />
          </div>
        ))}
      </div>
    );
  };

  return (
    <StoreLayout>
      <section className="store-hero rounded p-4 p-md-5 mb-4">
        <h1 className="display-6 fw-bold">New season laptops</h1>
        <p className="lead mb-3">Free shipping over $1,000</p>
        <Link to="/?category=Laptops" className="btn btn-warning">
          Shop
        </Link>
      </section>
      <div className="row">
        <aside className="col-lg-3 mb-3">
          <button
            type="button"
            className="btn btn-outline-secondary w-100 d-lg-none mb-3"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            Filters
          </button>
          <div className={filtersOpen ? '' : 'd-none d-lg-block'}>
            <CatalogFilters
              categories={categoriesOf(products)}
              brands={brandsOf(products)}
              query={query}
              onChange={setQuery}
            />
          </div>
        </aside>
        <section className="col-lg-9" aria-label="Catalog">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <span aria-label="Product Count">
              {error ? 0 : shown.length} {shown.length === 1 && !error ? 'product' : 'products'}
            </span>
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="catalog-sort" className="form-label mb-0">
                Sort:
              </label>
              <select
                id="catalog-sort"
                className="form-select form-select-sm w-auto"
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
