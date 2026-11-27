import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Minus, Plus, ShoppingCart } from 'lucide-react';
import { LoadingSpinner, ProductImage, StoreLayout } from '../components';
import { WishlistButton } from '../components/WishlistButton';
import { useCart } from '../contexts/useCart';
import { useCatalog } from '../contexts/useCatalog';
import { useNotificationContext } from '../contexts/useNotificationContext';
import { ServiceTiles } from '../features/catalog/ServiceTiles';
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
  const { products } = useCatalog();
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
        <div className="market-empty text-center py-5 px-3">
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
  // Other products in the same category, from the catalog already loaded for the header.
  const related = products.filter((other) => other.category === product.category && other.sku !== product.sku).slice(0, 4);

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
      <div className="product-detail">
        <div className="product-detail-media market-panel">
          <ProductImage imageUrl={product.imageUrl} alt={product.title} />
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
              SKU{' '}
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
                  <Minus size={16} aria-hidden="true" />
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
                  <Plus size={16} aria-hidden="true" />
                </button>
              </div>
              <button type="submit" className="btn btn-cta btn-lg product-add-button" aria-label="Add to Cart">
                <ShoppingCart size={18} aria-hidden="true" className="me-2" />
                Add to cart
              </button>
              <WishlistButton title={product.title} className="wishlist-button-boxed" />
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
      {related.length > 0 && (
        <section className="home-section mt-4" aria-labelledby="product-related-heading">
          <h2 className="home-section-title" id="product-related-heading">
            More in {product.category}
          </h2>
          <ul className="compact-products">
            {related.map((other) => (
              <li key={other.sku}>
                <Link to={`/products/${encodeURIComponent(other.sku)}`} className="compact-product">
                  <span className="compact-product-thumb">
                    <ProductImage imageUrl={other.imageUrl} alt="" shape="square" lazy />
                  </span>
                  <span className="compact-product-text">
                    <span className="compact-product-title">{other.title}</span>
                    <span className="compact-product-price price">{formatMoney(other.price)}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}
    </StoreLayout>
  );
}
