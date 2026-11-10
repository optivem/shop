// Product image placeholder: the ERP has no image field, so each product gets a tile with its brand name,
// coloured by a deterministic hash of the brand (storefront-ux §5). No external images.

import { brandColour } from './brand-colour';

interface ProductImageProps {
  brand: string;
  /** 4:3 on cards, 1:1 on the product page. */
  shape?: 'card' | 'square';
}

export function ProductImage({ brand, shape = 'card' }: Readonly<ProductImageProps>) {
  return (
    <div
      className={`product-image product-image-${shape}`}
      style={{ backgroundColor: brandColour(brand) }}
      role="img"
      aria-label={`${brand} product image`}
    >
      <span>{brand.toUpperCase()}</span>
    </div>
  );
}
