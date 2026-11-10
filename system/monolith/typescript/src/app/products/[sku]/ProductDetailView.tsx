"use client";

import { useState } from "react";
import { StoreLayout } from "@/components/StoreLayout";
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
      <div className="row g-4">
        <div className="col-md-5">
          <ProductImage brand={product.brand} shape="square" />
        </div>
        <div className="col-md-7">
          <h1 className="h2" aria-label="Product Title">
            {product.title}
          </h1>
          <p className="text-muted">
            by <span aria-label="Product Brand">{product.brand}</span> ·{" "}
            <span aria-label="Product Category">{product.category}</span> · SKU{" "}
            <span aria-label="Product SKU">{product.sku}</span>
          </p>
          <p className="price fs-3 fw-bold" aria-label="Display Price">
            {formatMoney(product.price)}
          </p>
          <form
            className="d-flex flex-wrap align-items-center gap-3 mb-4"
            onSubmit={(e) => {
              e.preventDefault();
              addToCart();
            }}
          >
            <label htmlFor="product-quantity" className="form-label mb-0">
              Qty
            </label>
            <div className="input-group quantity-stepper">
              <button type="button" className="btn btn-outline-secondary" aria-label="Decrease Quantity" onClick={() => step(-1)}>
                −
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
                +
              </button>
            </div>
            <button type="submit" className="btn btn-primary" aria-label="Add to Cart">
              Add to cart
            </button>
          </form>
          <hr />
          <p aria-label="Product Description">{product.description}</p>
          <p className="text-muted small mb-0">✓ Ships in 1–2 business days · ✓ 30-day returns</p>
        </div>
      </div>
    </StoreLayout>
  );
}
