import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
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
import { formatMoney } from '../utils/money';
import { clampQuantity, parseQuantity } from '../utils/quantity';

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
  const { t } = useTranslation('catalog');
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
      <StoreLayout title={t('productDetail.notFoundTitle')}>
        <div className="market-empty text-center py-5 px-3">
          <h1 className="h3">{t('productDetail.notFoundTitle')}</h1>
          <p className="text-muted">{t('productDetail.notFoundText', { sku })}</p>
          <Link to="/" className="btn btn-primary" aria-label={t('productDetail.backToCatalogAria')}>
            {t('productDetail.backToCatalog')}
          </Link>
        </div>
      </StoreLayout>
    );
  }

  if (state.kind !== 'found') {
    return (
      <StoreLayout title={t('productDetail.title')}>
        {state.kind === 'loading' ? (
          <LoadingSpinner message={t('productDetail.loading')} />
        ) : (
          <div className="alert alert-danger" role="alert">
            {t('page.productsUnavailable')}
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
      setError({ message: t('productDetail.quantityError') });
      return;
    }
    add(product.sku, parsed);
    setSuccess(t('page.addedToCart'));
  };

  return (
    <StoreLayout
      title={product.title}
      breadcrumbs={[
        { label: t('productDetail.home'), path: '/' },
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
              <span className="product-meta-brand" aria-label={t('productDetail.brandAria')}>
                {product.brand}
              </span>
              <span className="product-meta-sep" aria-hidden="true">
                /
              </span>
              <span aria-label={t('productDetail.categoryAria')}>{product.category}</span>
            </p>
            <h1 className="product-detail-title" aria-label={t('productDetail.titleAria')}>
              {product.title}
            </h1>
            <p className="product-meta mb-0">
              {t('productDetail.sku')}{' '}
              <span className="font-monospace text-body" aria-label={t('productDetail.skuAria')}>
                {product.sku}
              </span>
            </p>
            <p className="price product-detail-price" aria-label={t('productDetail.priceAria')}>
              {formatMoney(product.price)}
            </p>
            <div className="product-buy-row">
              <label htmlFor="product-quantity" className="visually-hidden">
                {t('productDetail.quantity')}
              </label>
              <div className="input-group quantity-stepper quantity-stepper-lg">
                <button type="button" className="btn btn-outline-secondary" aria-label={t('productDetail.decreaseAria')} onClick={() => step(-1)}>
                  <Minus size={16} aria-hidden="true" />
                </button>
                <input
                  id="product-quantity"
                  type="text"
                  inputMode="numeric"
                  className="form-control text-center"
                  aria-label={t('productDetail.quantityAria')}
                  value={quantity}
                  onChange={(e) => setQuantity(e.target.value)}
                />
                <button type="button" className="btn btn-outline-secondary" aria-label={t('productDetail.increaseAria')} onClick={() => step(1)}>
                  <Plus size={16} aria-hidden="true" />
                </button>
              </div>
              <button type="submit" className="btn btn-cta btn-lg product-add-button" aria-label={t('productDetail.addToCartAria')}>
                <ShoppingCart size={18} aria-hidden="true" className="me-2" />
                {t('productDetail.addToCart')}
              </button>
              <WishlistButton title={product.title} className="wishlist-button-boxed" />
            </div>
          </form>
          <ServiceTiles compact />
          <section className="market-panel product-about" aria-labelledby="product-about-heading">
            <h2 className="product-about-title" id="product-about-heading">
              {t('productDetail.aboutTitle')}
            </h2>
            <p className="product-detail-description mb-0" aria-label={t('productDetail.descriptionAria')}>
              {product.description}
            </p>
          </section>
        </div>
      </div>
      {related.length > 0 && (
        <section className="home-section mt-4" aria-labelledby="product-related-heading">
          <h2 className="home-section-title" id="product-related-heading">
            {t('productDetail.moreIn', { category: product.category })}
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
