"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { StoreLayout } from "@/components/StoreLayout";
import { ProductImage } from "@/components/StorefrontParts";
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
import { formatMoney } from "@/lib/storefront/money";
import type { Product } from "@/lib/storefront/types";

function rawParams(params: URLSearchParams): RawSearchParams {
  const raw: RawSearchParams = {};
  for (const key of new Set(params.keys())) {
    raw[key] = params.getAll(key);
  }
  return raw;
}

/** One catalog tile: image and title link to the product page; "Add to cart" adds quantity 1. */
function ProductCard({ product, onAddToCart }: Readonly<{ product: Product; onAddToCart: (sku: string) => void }>) {
  const productPath = `/products/${encodeURIComponent(product.sku)}`;
  return (
    <article className="product-card card h-100" aria-label={`Product ${product.sku}`}>
      <Link href={productPath} tabIndex={-1} aria-hidden="true">
        <ProductImage brand={product.brand} />
      </Link>
      <div className="card-body d-flex flex-column">
        <h3 className="h6 card-title mb-1">
          <Link href={productPath} className="product-title-link" aria-label="Product Title">
            {product.title}
          </Link>
        </h3>
        <div className="text-muted small mb-2" aria-label="Product Brand">
          {product.brand}
        </div>
        <div className="price fw-bold mb-3" aria-label="Display Price">
          {formatMoney(product.price)}
        </div>
        <button
          type="button"
          className="btn btn-primary mt-auto"
          aria-label={`Add ${product.sku} to Cart`}
          onClick={() => onAddToCart(product.sku)}
        >
          Add to cart
        </button>
      </div>
    </article>
  );
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
      <fieldset className="mb-4" aria-label="Category Filter">
        <legend className="h6">Category</legend>
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
      <fieldset className="mb-4" aria-label="Brand Filter">
        <legend className="h6">Brand</legend>
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
        <div className="text-center py-5">
          <p className="lead">No products match your search.</p>
          <Link href="/" className="btn btn-outline-primary" aria-label="Clear Filters">
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
        <Link href="/?category=Laptops" className="btn btn-warning">
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
          <div className={filtersOpen ? "" : "d-none d-lg-block"}>
            <CatalogFilters categories={categoriesOf(products)} brands={brandsOf(products)} query={query} onChange={setQuery} />
          </div>
        </aside>
        <section className="col-lg-9" aria-label="Catalog">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <span aria-label="Product Count">
              {unavailable ? 0 : shown.length} {shown.length === 1 && !unavailable ? "product" : "products"}
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
