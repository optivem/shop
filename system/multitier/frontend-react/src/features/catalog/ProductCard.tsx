import { useTranslation } from 'react-i18next';
import { Link } from 'react-router-dom';
import { ProductImage } from '../../components/ProductImage';
import { WishlistButton } from '../../components/WishlistButton';
import type { Product } from '../../types/api.types';
import { formatMoney } from '../../utils/money';

interface ProductCardProps {
  product: Product;
  onAddToCart: (sku: string) => void;
  /**
   * The catalog grid's cards carry the UI drivers' hooks (aria-labels, data-testids). The home page's
   * Bestsellers row reuses the card without them, so each hook still names exactly one element.
   */
  testHooks?: boolean;
}

/** One catalog tile: photo and title link to the product page; brand, description and price come from the catalog; "Add to cart" adds quantity 1. */
export function ProductCard({ product, onAddToCart, testHooks = true }: Readonly<ProductCardProps>) {
  const { t } = useTranslation('catalog');
  const productPath = `/products/${encodeURIComponent(product.sku)}`;
  return (
    <article className="product-card" aria-label={testHooks ? t('productCard.productAria', { sku: product.sku }) : product.title}>
      <div className="product-card-media">
        <Link to={productPath} tabIndex={-1} aria-hidden="true">
          <ProductImage imageUrl={product.imageUrl} alt={product.title} lazy />
        </Link>
        <WishlistButton title={product.title} className="product-card-wishlist" />
      </div>
      <div className="product-card-body">
        <div className="product-card-brand" aria-label={testHooks ? t('productCard.brandAria') : undefined}>
          {product.brand}
        </div>
        <h3 className="product-card-title">
          <Link to={productPath} className="product-title-link" data-testid={testHooks ? 'product-title' : undefined}>
            {product.title}
          </Link>
        </h3>
        {product.description && <p className="product-card-spec">{product.description}</p>}
        <div className="price product-card-price" aria-label={testHooks ? t('productCard.priceAria') : undefined}>
          {formatMoney(product.price)}
        </div>
        <button
          type="button"
          className="btn btn-cta w-100 mt-auto"
          data-testid={testHooks ? `add-to-cart-${product.sku}` : undefined}
          aria-label={testHooks ? undefined : t('productCard.addToCartAria', { title: product.title })}
          onClick={() => onAddToCart(product.sku)}
        >
          {t('productCard.addToCart')}
        </button>
      </div>
    </article>
  );
}
