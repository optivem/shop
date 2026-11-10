import { Link } from 'react-router-dom';
import { ProductImage } from '../../components/ProductImage';
import type { Product } from '../../types/api.types';
import { formatMoney } from '../../utils/money';

interface ProductCardProps {
  product: Product;
  onAddToCart: (sku: string) => void;
}

/** One catalog tile: image and title link to the product page; "Add to cart" adds quantity 1. */
export function ProductCard({ product, onAddToCart }: Readonly<ProductCardProps>) {
  const productPath = `/products/${encodeURIComponent(product.sku)}`;
  return (
    <article className="product-card card h-100" aria-label={`Product ${product.sku}`}>
      <Link to={productPath} tabIndex={-1} aria-hidden="true">
        <ProductImage brand={product.brand} />
      </Link>
      <div className="card-body d-flex flex-column">
        <h3 className="h6 card-title mb-1">
          <Link to={productPath} className="product-title-link" aria-label="Product Title">
            {product.title}
          </Link>
        </h3>
        <div className="text-muted small mb-2" aria-label="Product Brand">
          {product.brand}
        </div>
        <div className="price fw-bold mb-3" aria-label="Display Price">
          {formatMoney(product.price)}
        </div>
        <button
          type="button"
          className="btn btn-primary mt-auto"
          aria-label={`Add ${product.sku} to Cart`}
          onClick={() => onAddToCart(product.sku)}
        >
          Add to cart
        </button>
      </div>
    </article>
  );
}
