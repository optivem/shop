"use client";

import Link from "next/link";
import { categoriesOf } from "@/lib/storefront/catalog-query";
import { formatMoney } from "@/lib/storefront/money";
import type { Product } from "@/lib/storefront/types";
import { Icon } from "./Icon";
import { ProductCard } from "./ProductCard";
import { ServiceTiles } from "./ServiceTiles";
import { ProductImage } from "./StorefrontParts";

/** The hero's product. Its title, description and price are read from the catalog, never written here. */
const FEATURED_SKU = "DELL-U2723QE";

/**
 * Curated merchandising picks: the ERP has no sales figures or listing dates yet, so these rows are hand-picked
 * SKUs (any the catalog no longer carries are skipped). Swap them for real data once the backend provides it.
 */
const BESTSELLER_SKUS = ["DELL-XPS", "HP-15", "LG-27UL500", "LOGI-MX3S", "LOGI-MXKEYS"];
const NEW_ARRIVAL_SKUS = ["ASUS-ZENBOOK-14", "LENOVO-T14", "SAMSUNG-S24", "ANKER-HUB-7"];

const pick = (products: Product[], skus: string[]) =>
  skus.map((sku) => products.find((product) => product.sku === sku)).filter((p): p is Product => p !== undefined);

const productPath = (sku: string) => `/products/${encodeURIComponent(sku)}`;

interface HomeSectionsProps {
  products: Product[];
  onAddToCart: (sku: string) => void;
}

/**
 * The home page above the full catalog (Retail look): featured hero, service tiles, round-photo category
 * shortcuts, and the Bestsellers / New arrivals rows. Shown only on the unfiltered catalog. The cards here carry
 * none of the UI drivers' hooks, so the catalog grid stays the only place they appear.
 */
// Plain <img>: the hero photo is a fixed static file and the category photos are the same-origin ERP proxy.
/* eslint-disable @next/next/no-img-element */
export function HomeSections({ products, onAddToCart }: Readonly<HomeSectionsProps>) {
  const featured = products.find((product) => product.sku === FEATURED_SKU);
  const bestsellers = pick(products, BESTSELLER_SKUS);
  const newArrivals = pick(products, NEW_ARRIVAL_SKUS);
  const categories = categoriesOf(products);
  const photoOf = (category: string) => products.find((p) => p.category === category && p.imageUrl)?.imageUrl;

  return (
    <div className="home-sections">
      <section className="deal-hero" aria-label="Promotion">
        <div className="deal-hero-copy">
          <span className="badge badge-deal">Featured</span>
          <h1 className="deal-hero-title">A 4K workspace, sorted.</h1>
          {featured ? (
            <p className="deal-hero-lead">
              {featured.title}: {featured.description || featured.category}.{" "}
              <strong className="deal-hero-price price">{formatMoney(featured.price)}</strong>
            </p>
          ) : (
            <p className="deal-hero-lead">Sharp monitors for a cleaner desk.</p>
          )}
          <Link href={featured ? productPath(featured.sku) : "/?category=Monitors"} className="btn deal-hero-cta">
            {featured ? "See the monitor" : "Shop monitors"}
          </Link>
        </div>
        <div className="deal-hero-media">
          <img className="deal-hero-photo" src="/images/promo/monitor.jpg" alt="A 27-inch monitor on a tidy desk" />
        </div>
      </section>

      <ServiceTiles />

      {categories.length > 0 && (
        <section className="home-section" aria-labelledby="home-categories-heading">
          <h2 className="home-section-title" id="home-categories-heading">
            Shop by category
          </h2>
          <ul className="category-shortcuts">
            {categories.map((category) => {
              const photo = photoOf(category);
              return (
                <li key={category}>
                  <Link href={`/?category=${encodeURIComponent(category)}`} className="category-shortcut">
                    {photo ? (
                      <img className="category-shortcut-photo" src={photo} alt="" width={56} height={56} loading="lazy" />
                    ) : (
                      <span className="category-shortcut-photo" aria-hidden="true" />
                    )}
                    {category}
                  </Link>
                </li>
              );
            })}
          </ul>
        </section>
      )}

      {bestsellers.length > 0 && (
        <section className="home-section" aria-labelledby="home-bestsellers-heading">
          <div className="home-section-head">
            <h2 className="home-section-title" id="home-bestsellers-heading">
              Bestsellers
            </h2>
            <a href="#catalog" className="home-section-link">
              See all products <Icon name="arrow-right" size={16} />
            </a>
          </div>
          <div className="product-row">
            {bestsellers.map((product) => (
              <ProductCard key={product.sku} product={product} onAddToCart={onAddToCart} testHooks={false} />
            ))}
          </div>
        </section>
      )}

      {newArrivals.length > 0 && (
        <section className="home-section" aria-labelledby="home-new-heading">
          <h2 className="home-section-title" id="home-new-heading">
            New arrivals
          </h2>
          <ul className="compact-products">
            {newArrivals.map((product) => (
              <li key={product.sku}>
                <Link href={productPath(product.sku)} className="compact-product">
                  <span className="compact-product-thumb">
                    <ProductImage imageUrl={product.imageUrl} brand={product.brand} alt="" shape="square" lazy />
                  </span>
                  <span className="compact-product-text">
                    <span className="compact-product-tag">New</span>
                    <span className="compact-product-title">{product.title}</span>
                    <span className="compact-product-price price">{formatMoney(product.price)}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  );
}
/* eslint-enable @next/next/no-img-element */
