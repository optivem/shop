import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { LoadingSpinner, ProductImage, StoreLayout } from '../components';
import { useCart } from '../contexts/useCart';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { productService } from '../services/product-service';
import type { Product } from '../types/api.types';
import { ADDED_TO_CART, PRODUCTS_UNAVAILABLE } from '../utils/messages';
import { formatMoney } from '../utils/money';
import { clampQuantity, parseQuantity, QUANTITY_ERROR } from '../utils/quantity';

type ProductState =
  | { kind: 'loading' }
  | { kind: 'found'; product: Product }
  | { kind: 'not-found' }
  | { kind: 'unavailable' };

interface Loaded {
  sku: string;
  state: ProductState;
}

/** `/products/{sku}`: one product's details, with a quantity and "Add to cart" (UC-007). */
export function ProductDetail() {
  const { sku = '' } = useParams<{ sku: string }>();
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [quantity, setQuantity] = useState('1');
  const { add } = useCart();
  const { setSuccess, setError } = useNotificationContext();

  useEffect(() => {
    let ignore = false;
    void productService.viewProduct(sku).then((result) => {
      if (ignore) return;
      let state: ProductState;
      if (result.success) state = { kind: 'found', product: result.data };
      else if (result.error.status === 404) state = { kind: 'not-found' };
      else state = { kind: 'unavailable' };
      setLoaded({ sku, state });
      setQuantity('1');
    });
    return () => {
      ignore = true;
    };
  }, [sku]);

  const state: ProductState = loaded?.sku === sku ? loaded.state : { kind: 'loading' };

  if (state.kind === 'not-found') {
    return (
      <StoreLayout title="Product not found">
        <div className="text-center py-5">
          <h1 className="h3">Product not found</h1>
          <p className="text-muted">We couldn't find a product with SKU {sku}.</p>
          <Link to="/" className="btn btn-primary" aria-label="Back to Catalog">
            Back to the catalog
          </Link>
        </div>
      </StoreLayout>
    );
  }

  if (state.kind !== 'found') {
    return (
      <StoreLayout title="Product">
        {state.kind === 'loading' ? (
          <LoadingSpinner message="Loading product..." />
        ) : (
          <div className="alert alert-danger" role="alert">
            {PRODUCTS_UNAVAILABLE}
          </div>
        )}
      </StoreLayout>
    );
  }

  const { product } = state;

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
        { label: 'Home', path: '/' },
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
            by <span aria-label="Product Brand">{product.brand}</span> ·{' '}
            <span aria-label="Product Category">{product.category}</span> · SKU{' '}
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
