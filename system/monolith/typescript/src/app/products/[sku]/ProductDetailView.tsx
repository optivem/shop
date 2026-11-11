"use client";

import { useState } from "react";
import { Icon } from "@/components/Icon";
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
      <div className="row g-4 align-items-start">
        <div className="col-lg-7">
          <div className="market-panel p-3">
            <ProductImage brand={product.brand} shape="square" />
          </div>
        </div>
        <div className="col-lg-5 d-flex flex-column gap-3">
          <div className="market-panel p-4">
            <p className="product-meta mb-2">
              <span className="product-meta-brand" aria-label="Product Brand">
                {product.brand}
              </span>{" "}
              · <span aria-label="Product Category">{product.category}</span>
            </p>
            <h1 className="product-detail-title" aria-label="Product Title">
              {product.title}
            </h1>
            <p className="product-meta mb-3">
              SKU{" "}
              <span className="font-monospace text-body" aria-label="Product SKU">
                {product.sku}
              </span>
            </p>
            <p className="price product-detail-price" aria-label="Display Price">
              {formatMoney(product.price)}
            </p>
            <p className="product-detail-description mb-0" aria-label="Product Description">
              {product.description}
            </p>
          </div>
          <form
            className="market-panel p-4 d-flex flex-column gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              addToCart();
            }}
          >
            <div className="d-flex align-items-center justify-content-between gap-3">
              <label htmlFor="product-quantity" className="form-label fw-bold mb-0">
                Quantity
              </label>
              <div className="input-group quantity-stepper">
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
            </div>
            <button type="submit" className="btn btn-primary btn-lg w-100" aria-label="Add to Cart">
              <Icon name="shopping-cart" size={18} className="me-2" />
              Add to cart
            </button>
            <ul className="product-perks list-unstyled mb-0">
              <li>
                <Icon name="check" /> Free shipping on orders over $1,000
              </li>
              <li>
                <Icon name="check" /> 30-day returns
              </li>
            </ul>
          </form>
        </div>
      </div>
    </StoreLayout>
  );
}
