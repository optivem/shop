import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { SlidersHorizontal } from 'lucide-react';
import { Link, useSearchParams } from 'react-router-dom';
import { StoreLayout } from '../components';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { CatalogFilters } from '../features/catalog/CatalogFilters';
import { DemoWakeUp } from '../features/catalog/DemoWakeUp';
import { HomeSections } from '../features/catalog/HomeSections';
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

/** The catalog's shape with no data: what the shop looks like behind the wake-up dialog until the products arrive. */
function CatalogSkeleton() {
  return (
    <div className="placeholder-glow">
      <span className="placeholder catalog-skeleton-heading" />
      <div className="product-grid">
        {Array.from({ length: 8 }, (_, i) => (
          <div key={i} className="product-card">
            <span className="placeholder catalog-skeleton-image" />
            <span className="placeholder col-4" />
            <span className="placeholder col-10" />
            <span className="placeholder col-6" />
            <span className="placeholder col-12 catalog-skeleton-button" />
          </div>
        ))}
      </div>
    </div>
  );
}

/** The home page: browse, search, filter and sort the catalog (UC-006); add a product with quantity 1. */
export function Catalog() {
  const { t } = useTranslation('catalog');
  const { products, isLoading, error } = useCatalog();
  const { add } = useCart();
  const { setSuccess } = useNotificationContext();
  const [searchParams, setSearchParams] = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  // The query lives in the page address (BR-7), so a filtered view can be bookmarked.
  const query = parseCatalogQuery(searchParams);
  const shown = applyCatalogQuery(products, query);
  const setQuery = (next: CatalogQuery) => setSearchParams(toSearchParams(next));
  // The home sections (hero, shortcuts, rows) only show on the plain catalog; a search or filter goes straight to results.
  const isHome = query.q.trim() === '' && query.category === '' && query.brands.length === 0;
  const heading = query.category || (query.q.trim() ? t('page.searchResults') : t('page.allProducts'));

  const addToCart = (sku: string) => {
    add(sku, 1);
    setSuccess(t('page.addedToCart'));
  };

  // The first load (a cold start on the demo deployment) shows the wake-up card as a dialog over the shop,
  // which waits greyed out behind it with placeholder cards: an empty sidebar and "0 products" would look broken.
  if (isLoading && products.length === 0) {
    return (
      <>
        <div className="wake-up-backdrop-page" inert aria-hidden="true">
          <StoreLayout>
            <CatalogSkeleton />
          </StoreLayout>
        </div>
        <DemoWakeUp />
      </>
    );
  }

  const renderResults = () => {
    if (error) {
      return (
        <div className="alert alert-danger" role="alert">
          {t('page.productsUnavailable')}
        </div>
      );
    }
    if (shown.length === 0) {
      return (
        <div className="market-empty text-center py-5 px-3">
          <p className="lead">{t('page.noMatch')}</p>
          <Link to="/" className="btn btn-outline-primary" aria-label={t('page.clearFiltersAria')}>
            {t('page.clearFilters')}
          </Link>
        </div>
      );
    }
    return (
      <div className="product-grid" aria-label={t('page.productListAria')}>
        {shown.map((product) => (
          <ProductCard key={product.sku} product={product} onAddToCart={addToCart} />
        ))}
      </div>
    );
  };

  return (
    <StoreLayout>
      {isHome && products.length > 0 && !error && <HomeSections products={products} onAddToCart={addToCart} />}
      {isHome ? (
        <h2 className="catalog-heading" id="catalog">
          {heading}
        </h2>
      ) : (
        <h1 className="catalog-heading" id="catalog">
          {heading}
        </h1>
      )}
      <div className="catalog-layout">
        <aside className="catalog-sidebar">
          <button
            type="button"
            className="btn btn-outline-secondary w-100 d-lg-none"
            aria-expanded={filtersOpen}
            onClick={() => setFiltersOpen((open) => !open)}
          >
            <SlidersHorizontal size={16} aria-hidden="true" className="me-2" />
            {t('page.filters')}
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
        <section className="catalog-results" aria-label={t('page.catalogAria')}>
          <div className="catalog-toolbar">
            <p className="mb-0">
              <strong aria-label={t('page.productCountAria')}>
                {t('page.productCount', { count: error ? 0 : shown.length })}
              </strong>{' '}
              <span className="text-body-secondary">{t('page.inCategory', { category: query.category || t('page.allProducts') })}</span>
            </p>
            <div className="d-flex align-items-center gap-2">
              <label htmlFor="catalog-sort" className="form-label mb-0 text-body-secondary">
                {t('page.sortBy')}
              </label>
              <select
                id="catalog-sort"
                className="form-select w-auto"
                aria-label={t('page.sortAria')}
                value={query.sort}
                onChange={(e) => setQuery({ ...query, sort: e.target.value as SortOrder })}
              >
                {SORT_OPTIONS.map((option) => (
                  <option key={option.value} value={option.value}>
                    {t(option.labelKey)}
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
