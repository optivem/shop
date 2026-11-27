"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { HomeSections } from "@/components/HomeSections";
import { Icon } from "@/components/Icon";
import { ProductCard } from "@/components/ProductCard";
import { StoreLayout } from "@/components/StoreLayout";
import { useCart, useCatalog, useNotification } from "@/components/StorefrontProviders";
import {
  applyCatalogQuery,
  brandsOf,
  catalogPath,
  categoriesOf,
  parseCatalogQuery,
  SORT_OPTIONS,
  type CatalogQuery,
  type RawSearchParams,
  type SortOrder,
} from "@/lib/storefront/catalog-query";
import { ADDED_TO_CART, PRODUCTS_UNAVAILABLE } from "@/lib/storefront/messages";

function rawParams(params: URLSearchParams): RawSearchParams {
  const raw: RawSearchParams = {};
  for (const key of new Set(params.keys())) {
    raw[key] = params.getAll(key);
  }
  return raw;
}

interface CatalogFiltersProps {
  categories: string[];
  brands: string[];
  query: CatalogQuery;
  onChange: (query: CatalogQuery) => void;
}

/** Category (one, or All) and brand (any number) filters for the catalog. */
function CatalogFilters({ categories, brands, query, onChange }: Readonly<CatalogFiltersProps>) {
  const toggleBrand = (brand: string, checked: boolean) => {
    const next = checked ? [...query.brands, brand] : query.brands.filter((b) => b !== brand);
    onChange({ ...query, brands: brands.filter((b) => next.includes(b)) });
  };

  return (
    <div className="catalog-filters">
      <fieldset aria-label="Category Filter">
        <legend className="catalog-filter-legend">Category</legend>
        {["", ...categories].map((category) => {
          const id = `category-${category || "all"}`;
          return (
            <div className="form-check" key={id}>
              <input
                className="form-check-input"
                type="radio"
                name="category"
                id={id}
                value={category}
                checked={query.category === category}
                onChange={() => onChange({ ...query, category })}
              />
              <label className="form-check-label" htmlFor={id}>
                {category || "All"}
              </label>
            </div>
          );
        })}
      </fieldset>
      <fieldset aria-label="Brand Filter">
        <legend className="catalog-filter-legend">Brand</legend>
        {brands.map((brand) => {
          const id = `brand-${brand}`;
          return (
            <div className="form-check" key={id}>
              <input
                className="form-check-input"
                type="checkbox"
                name="brand"
                id={id}
                value={brand}
                checked={query.brands.includes(brand)}
                onChange={(e) => toggleBrand(brand, e.target.checked)}
              />
              <label className="form-check-label" htmlFor={id}>
                {brand}
              </label>
            </div>
          );
        })}
      </fieldset>
    </div>
  );
}

/** The home page: browse, search, filter and sort the catalog (UC-006); add a product with quantity 1. */
export function Catalog() {
  const { products, unavailable } = useCatalog();
  const { add } = useCart();
  const { setSuccess } = useNotification();
  const router = useRouter();
  const searchParams = useSearchParams();
  const [filtersOpen, setFiltersOpen] = useState(false);

  // The query lives in the page address (BR-7), so a filtered view can be bookmarked.
  const query = parseCatalogQuery(rawParams(searchParams));
  const shown = applyCatalogQuery(products, query);
  const setQuery = (next: CatalogQuery) => router.push(catalogPath(next), { scroll: false });
  // The home sections (hero, shortcuts, rows) only show on the plain catalog; a search or filter goes straight to results.
  const isHome = query.q.trim() === "" && query.category === "" && query.brands.length === 0;
  const heading = query.category || (query.q.trim() ? "Search results" : "All products");

  const addToCart = (sku: string) => {
    add(sku, 1);
    setSuccess(ADDED_TO_CART);
  };

  const renderResults = () => {
    if (unavailable) {
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
          <Link href="/" className="btn btn-outline-primary" aria-label="Clear Filters">
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
      {isHome && products.length > 0 && !unavailable && <HomeSections products={products} onAddToCart={addToCart} />}
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
            <Icon name="sliders-horizontal" className="me-2" />
            Filters
          </button>
          <div className={filtersOpen ? "mt-3 mt-lg-0" : "d-none d-lg-block"}>
            <CatalogFilters categories={categoriesOf(products)} brands={brandsOf(products)} query={query} onChange={setQuery} />
          </div>
        </aside>
        <section className="catalog-results" aria-label="Catalog">
          <div className="catalog-toolbar">
            <p className="mb-0">
              <strong aria-label="Product Count">
                {unavailable ? 0 : shown.length} {shown.length === 1 && !unavailable ? "product" : "products"}
              </strong>{" "}
              <span className="text-body-secondary">in {query.category || "All products"}</span>
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
