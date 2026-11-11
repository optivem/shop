"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Icon } from "@/components/Icon";
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

/**
 * The home hero and two promo tiles. They are the only places that show stock photos (public/images/promo/,
 * credited in the store footer). Each tile links to a category the catalog actually carries.
 */
const SIDE_TILES = [
  { label: "Shop monitors", path: "/?category=Monitors", image: "/images/promo/monitor.jpg", alt: "Monitor on a desk" },
  { label: "Build your desk setup", path: "/?category=Accessories", image: "/images/promo/keyboard.jpg", alt: "Mechanical keyboard" },
];

// Plain <img>: the photos are fixed static files and this app does not run next/image's optimiser.
/* eslint-disable @next/next/no-img-element */
function PromoTiles() {
  return (
    <section className="promo-tiles mb-4" aria-label="Promotions">
      <div className="promo-hero">
        <img className="promo-photo promo-photo-hero" src="/images/promo/laptop.jpg" alt="Laptop on a white desk" />
        <div className="promo-hero-panel">
          <span className="badge badge-deal">New season</span>
          <h1 className="promo-hero-title">New season laptops</h1>
          <p className="lead">Free shipping over $1,000</p>
          <Link href="/?category=Laptops" className="btn btn-primary">
            Shop laptops
          </Link>
        </div>
      </div>
      <div className="promo-side">
        {SIDE_TILES.map((tile) => (
          <Link key={tile.path} href={tile.path} className="promo-tile">
            <img className="promo-photo" src={tile.image} alt={tile.alt} loading="lazy" />
            <span className="promo-tile-label">
              {tile.label} <Icon name="arrow-right" />
            </span>
          </Link>
        ))}
      </div>
    </section>
  );
}
/* eslint-enable @next/next/no-img-element */

/** One catalog tile: image and title link to the product page; brand, description and price come from the catalog; "Add to cart" adds quantity 1. */
function ProductCard({ product, onAddToCart }: Readonly<{ product: Product; onAddToCart: (sku: string) => void }>) {
  const productPath = `/products/${encodeURIComponent(product.sku)}`;
  return (
    <article className="product-card card h-100" aria-label={`Product ${product.sku}`}>
      <Link href={productPath} className="product-card-media" tabIndex={-1} aria-hidden="true">
        <ProductImage brand={product.brand} />
      </Link>
      <div className="product-card-body">
        <div className="product-card-brand" aria-label="Product Brand">
          {product.brand}
        </div>
        <h3 className="product-card-title">
          <Link href={productPath} className="product-title-link" aria-label="Product Title">
            {product.title}
          </Link>
        </h3>
        {product.description && <p className="product-card-spec">{product.description}</p>}
        <div className="price product-card-price" aria-label="Display Price">
          {formatMoney(product.price)}
        </div>
        <button
          type="button"
          className="btn btn-primary w-100 mt-auto"
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
      <PromoTiles />
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
