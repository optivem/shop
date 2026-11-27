"use client";

import { useState } from "react";
import { Icon } from "@/components/Icon";
import { StoreLayout } from "@/components/StoreLayout";
import { ServiceTiles } from "@/components/ServiceTiles";
import { ProductImage } from "@/components/StorefrontParts";
import { useCart, useNotification } from "@/components/StorefrontProviders";
import { ADDED_TO_CART, PRODUCTS_UNAVAILABLE } from "@/lib/storefront/messages";
import { formatMoney } from "@/lib/storefront/money";
import { clampQuantity, parseQuantity, QUANTITY_ERROR } from "@/lib/storefront/quantity";
import type { Product } from "@/lib/storefront/types";

/** One product's details, with a quantity and "Add to cart" (UC-007); null when the ERP is unavailable. */
export function ProductDetail({ product }: Readonly<{ product: Product | null }>) {
  const [quantity, setQuantity] = useState("1");
  const { add } = useCart();
  const { setSuccess, setError } = useNotification();

  if (!product) {
    return (
      <StoreLayout title="Product">
        <div className="alert alert-danger" role="alert">
          {PRODUCTS_UNAVAILABLE}
        </div>
      </StoreLayout>
    );
  }

  const step = (delta: number) => {
    setQuantity(String(clampQuantity((parseQuantity(quantity) ?? 1) + delta)));
  };

  const addToCart = () => {
    const parsed = parseQuantity(quantity);
    if (parsed === undefined) {
      setError({ message: QUANTITY_ERROR });
      return;
    }
    add(product.sku, parsed);
    setSuccess(ADDED_TO_CART);
  };

  return (
    <StoreLayout
      title={product.title}
      breadcrumbs={[
        { label: "Home", path: "/" },
        { label: product.category, path: `/?category=${encodeURIComponent(product.category)}` },
        { label: product.title },
      ]}
    >
      <div className="product-detail">
        <div className="product-detail-media market-panel">
          <ProductImage imageUrl={product.imageUrl} brand={product.brand} alt={product.title} />
        </div>
        <div className="product-detail-info">
          <form
            className="market-panel product-buy-box"
            onSubmit={(e) => {
              e.preventDefault();
              addToCart();
            }}
          >
            <p className="product-meta mb-0">
              <span className="product-meta-brand" aria-label="Product Brand">
                {product.brand}
              </span>
              <span className="product-meta-sep" aria-hidden="true">
                /
              </span>
              <span aria-label="Product Category">{product.category}</span>
            </p>
            <h1 className="product-detail-title" aria-label="Product Title">
              {product.title}
            </h1>
            <p className="product-meta mb-0">
              SKU{" "}
              <span className="font-monospace text-body" aria-label="Product SKU">
                {product.sku}
              </span>
            </p>
            <p className="price product-detail-price" aria-label="Display Price">
              {formatMoney(product.price)}
            </p>
            <div className="product-buy-row">
              <label htmlFor="product-quantity" className="visually-hidden">
                Quantity
              </label>
              <div className="input-group quantity-stepper quantity-stepper-lg">
                <button type="button" className="btn btn-outline-secondary" aria-label="Decrease Quantity" onClick={() => step(-1)}>
                  <Icon name="minus" />
                </button>
                <input
                  id="product-quantity"
                  type="text"
                  inputMode="numeric"
                  className="form-control text-center"
                  aria-label="Product Quantity"
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                />
                <button type="button" className="btn btn-outline-secondary" aria-label="Increase Quantity" onClick={() => step(1)}>
                  <Icon name="plus" />
                </button>
              </div>
              <button type="submit" className="btn btn-cta btn-lg product-add-button" aria-label="Add to Cart">
                <Icon name="shopping-cart" size={18} className="me-2" />
                Add to cart
              </button>
            </div>
          </form>
          <ServiceTiles compact />
          <section className="market-panel product-about" aria-labelledby="product-about-heading">
            <h2 className="product-about-title" id="product-about-heading">
              About this product
            </h2>
            <p className="product-detail-description mb-0" aria-label="Product Description">
              {product.description}
            </p>
          </section>
        </div>
      </div>
    </StoreLayout>
  );
}
