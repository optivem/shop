"use client";

import Link from "next/link";
import { formatMoney } from "@/lib/storefront/money";
import type { Product } from "@/lib/storefront/types";
import { ProductImage } from "./StorefrontParts";

interface ProductCardProps {
  product: Product;
  onAddToCart: (sku: string) => void;
  /**
   * The catalog grid's cards carry the UI drivers' hooks (aria-labels, data-testids). The home page's
   * Bestsellers row reuses the card without them, so each hook still names exactly one element.
   */
  testHooks?: boolean;
}

/** One catalog tile: image and title link to the product page; brand, description and price come from the catalog; "Add to cart" adds quantity 1. */
export function ProductCard({ product, onAddToCart, testHooks = true }: Readonly<ProductCardProps>) {
  const productPath = `/products/${encodeURIComponent(product.sku)}`;
  return (
    <article className="product-card card h-100" aria-label={testHooks ? `Product ${product.sku}` : product.title}>
      <div className="product-card-media">
        <Link href={productPath} tabIndex={-1} aria-hidden="true">
          <ProductImage imageUrl={product.imageUrl} brand={product.brand} alt={product.title} lazy />
        </Link>
      </div>
      <div className="product-card-body">
        <div className="product-card-brand" aria-label={testHooks ? "Product Brand" : undefined}>
          {product.brand}
        </div>
        <h3 className="product-card-title">
          <Link href={productPath} className="product-title-link" data-testid={testHooks ? "product-title" : undefined}>
            {product.title}
          </Link>
        </h3>
        {product.description && <p className="product-card-spec">{product.description}</p>}
        <div className="price product-card-price" aria-label={testHooks ? "Display Price" : undefined}>
          {formatMoney(product.price)}
        </div>
        <button
          type="button"
          className="btn btn-cta w-100 mt-auto"
          data-testid={testHooks ? `add-to-cart-${product.sku}` : undefined}
          aria-label={testHooks ? undefined : `Add ${product.title} to cart`}
          onClick={() => onAddToCart(product.sku)}
        >
          Add to cart
        </button>
      </div>
    </article>
  );
}
