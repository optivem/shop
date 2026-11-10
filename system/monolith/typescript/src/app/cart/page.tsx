"use client";

import { useEffect } from "react";
import Link from "next/link";
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
        <div className="text-center py-5">
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
      <div className="row g-4">
        <div className="col-lg-8">
          <div className="table-responsive">
            <table className="table align-middle cart-table" aria-label="Cart Lines">
              <thead>
                <tr>
                  <th scope="col">Product</th>
                  <th scope="col" className="text-end">
                    Unit price
                  </th>
                  <th scope="col">Quantity</th>
                  <th scope="col" className="text-end">
                    Line total
                  </th>
                  <th scope="col">
                    <span className="visually-hidden">Remove</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {lines.map((line) => (
                  <tr key={line.sku} aria-label={`Cart Line ${line.sku}`}>
                    <td>
                      <div className="d-flex align-items-center gap-3">
                        <div className="cart-thumb">
                          <ProductImage brand={line.product?.brand ?? line.sku} />
                        </div>
                        <div>
                          {line.product ? (
                            <Link href={`/products/${encodeURIComponent(line.sku)}`} aria-label={`Title ${line.sku}`}>
                              {line.product.title}
                            </Link>
                          ) : (
                            <span aria-label={`Title ${line.sku}`}>{line.sku}</span>
                          )}
                          {!line.product && <div className="small text-danger">No longer available</div>}
                        </div>
                      </div>
                    </td>
                    <td className="text-end price" aria-label={`Unit Price ${line.sku}`}>
                      {line.product ? formatMoney(line.product.price) : "-"}
                    </td>
                    <td>
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
                    <td className="text-end price" aria-label={`Line Total ${line.sku}`}>
                      {formatCents(line.lineTotalCents)}
                    </td>
                    <td className="text-end">
                      <button
                        type="button"
                        className="btn btn-sm btn-outline-danger"
                        aria-label={`Remove ${line.sku}`}
                        onClick={() => cart.remove(line.sku)}
                      >
                        ✕
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <Link href="/">← Continue shopping</Link>
        </div>
        <div className="col-lg-4">
          <div className="card shadow-sm">
            <div className="card-body">
              <h2 className="h5">Order summary</h2>
              <div className="d-flex justify-content-between">
                <span>Subtotal</span>
                <span className="price fw-bold" aria-label="Cart Subtotal">
                  {formatCents(subtotalCents)}
                </span>
              </div>
              <p className="text-muted small mt-2">Tax and discounts are calculated at checkout.</p>
              {/* A full page load: checkout is a protected page the server may send to the login first. */}
              <a href="/checkout" className="btn btn-primary w-100" aria-label="Proceed to Checkout">
                Proceed to checkout
              </a>
            </div>
          </div>
        </div>
      </div>
    );
  };

  return (
    <StoreLayout title="Cart" breadcrumbs={[{ label: "Home", path: "/" }, { label: "Cart" }]}>
      <h1 className="h3 mb-4">
        Shopping cart{" "}
        {itemCount > 0 && <span aria-label="Cart Line Count">({itemCount} {itemCount === 1 ? "item" : "items"})</span>}
      </h1>
      {renderContent()}
    </StoreLayout>
  );
}
