"use client";

import { useEffect } from "react";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { LoadingSpinner, StoreLayout } from "@/components/StoreLayout";
import { ProductImage, QuantityStepper } from "@/components/StorefrontParts";
import { useCart, useCatalog, useNotification } from "@/components/StorefrontProviders";
import { priceCart } from "@/lib/storefront/cart-lines";
import { PRODUCTS_UNAVAILABLE } from "@/lib/storefront/messages";
import { formatCents, formatMoney } from "@/lib/storefront/money";
import { QUANTITY_ERROR } from "@/lib/storefront/quantity";

/** `/cart`: the cart lines at current catalog prices, with quantity changes, removal and the subtotal (UC-008). */
export default function CartPage() {
  const cart = useCart();
  const { products, isLoading, unavailable, reload } = useCatalog();
  const { setError, clearNotification } = useNotification();

  // Prices are re-read from the catalog whenever the cart is shown.
  useEffect(() => {
    reload();
  }, [reload]);

  const { lines, subtotalCents } = priceCart(cart.lines, products);
  const itemCount = cart.lines.length;

  const renderContent = () => {
    if (itemCount === 0) {
      return (
        <div className="market-empty text-center py-5 px-3">
          <Icon name="shopping-cart" size={40} className="mb-3 text-body-secondary" />
          <p className="lead">Your cart is empty.</p>
          <Link href="/" className="btn btn-primary">
            Start shopping
          </Link>
        </div>
      );
    }
    if (unavailable) {
      return (
        <div className="alert alert-danger" role="alert">
          {PRODUCTS_UNAVAILABLE}
        </div>
      );
    }
    if (isLoading && products.length === 0) return <LoadingSpinner message="Loading cart..." />;
    return (
      <div className="row g-4 align-items-start">
        <div className="col-lg-8">
          <div className="market-panel px-3 px-md-4 py-2">
            <table className="table align-middle mb-0 cart-table" aria-label="Cart Lines">
              <thead>
                <tr>
                  <th scope="col">Product</th>
                  <th scope="col">Quantity</th>
                  <th scope="col" className="text-end">
                    Line total
                  </th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line) => (
                  <tr key={line.sku} aria-label={`Cart Line ${line.sku}`}>
                    <td className="cart-cell-product">
                      <div className="d-flex align-items-center gap-3">
                        <div className="cart-thumb">
                          <ProductImage brand={line.product?.brand ?? line.sku} shape="square" />
                        </div>
                        <div className="min-w-0">
                          {line.product ? (
                            <Link
                              href={`/products/${encodeURIComponent(line.sku)}`}
                              className="cart-line-title"
                              aria-label={`Title ${line.sku}`}
                            >
                              {line.product.title}
                            </Link>
                          ) : (
                            <span className="cart-line-title" aria-label={`Title ${line.sku}`}>
                              {line.sku}
                            </span>
                          )}
                          <div className="cart-line-meta">
                            {line.product ? (
                              <>
                                SKU {line.sku} ·{" "}
                                <span className="price" aria-label={`Unit Price ${line.sku}`}>
                                  {formatMoney(line.product.price)}
                                </span>{" "}
                                each
                              </>
                            ) : (
                              <>
                                <span className="text-danger">No longer available</span> ·{" "}
                                <span aria-label={`Unit Price ${line.sku}`}>-</span>
                              </>
                            )}
                          </div>
                          <button
                            type="button"
                            className="btn btn-link cart-remove"
                            aria-label={`Remove ${line.sku}`}
                            onClick={() => cart.remove(line.sku)}
                          >
                            <Icon name="trash-2" size={14} className="me-1" />
                            Remove
                          </button>
                        </div>
                      </div>
                    </td>
                    <td className="cart-cell-quantity">
                      <QuantityStepper
                        quantity={line.quantity}
                        onChange={(quantity) => {
                          clearNotification();
                          cart.setQuantity(line.sku, quantity);
                        }}
                        onInvalid={() => setError({ message: QUANTITY_ERROR })}
                        ariaLabel={`Quantity for ${line.sku}`}
                        decreaseLabel={`Decrease Quantity for ${line.sku}`}
                        increaseLabel={`Increase Quantity for ${line.sku}`}
                      />
                    </td>
                    <td className="cart-cell-total text-end price" aria-label={`Line Total ${line.sku}`}>
                      {formatCents(line.lineTotalCents)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <div className="col-lg-4">
          <div className="market-panel p-4 d-flex flex-column gap-3">
            <h2 className="h5 mb-0">Order summary</h2>
            <div className="d-flex justify-content-between cart-summary-row">
              <span>Subtotal</span>
              <span className="price fw-bold" aria-label="Cart Subtotal">
                {formatCents(subtotalCents)}
              </span>
            </div>
            <p className="text-body-secondary small mb-0">Tax and discounts are calculated at checkout.</p>
            {/* A full page load: checkout is a protected page the server may send to the login first. */}
            <a href="/checkout" className="btn btn-primary btn-lg w-100" aria-label="Proceed to Checkout">
              Proceed to checkout
            </a>
            <Link href="/" className="cart-continue">
              Continue shopping
            </Link>
          </div>
        </div>
      </div>
    );
  };

  return (
    <StoreLayout title="Cart" breadcrumbs={[{ label: "Home", path: "/" }, { label: "Cart" }]}>
      <h1 className="h2 mb-4">
        Shopping cart{" "}
        {itemCount > 0 && (
          <span className="cart-line-count" aria-label="Cart Line Count">
            ({itemCount} {itemCount === 1 ? "item" : "items"})
          </span>
        )}
      </h1>
      {renderContent()}
    </StoreLayout>
  );
}
