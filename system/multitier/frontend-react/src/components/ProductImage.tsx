// The product photo the ERP owns, served by the shop backend at the product's imageUrl (ERP -> backend -> UI).
// A neutral placeholder is only a defensive fallback: imageUrl is null, or the image fails to load.

import { useState } from 'react';
import { Package } from 'lucide-react';

interface ProductImageProps {
  imageUrl: string | null | undefined;
  /** What the photo shows (the product title). Empty for a thumbnail that sits next to the product's own title. */
  alt: string;
  /** 4:3 on cards and the product page (the photos are 600x450), 1:1 for cart and checkout thumbnails. */
  shape?: 'card' | 'square';
  /** Catalog grids lazy-load; the product page loads its one photo straight away. */
  lazy?: boolean;
}

export function ProductImage({ imageUrl, alt, shape = 'card', lazy = false }: Readonly<ProductImageProps>) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null);
  const className = `product-image product-image-${shape}`;

  if (!imageUrl || failedUrl === imageUrl) {
    return (
      <div
        className={`${className} product-image-placeholder`}
        {...(alt ? { role: 'img', 'aria-label': alt } : { 'aria-hidden': true })}
      >
        <Package size={32} strokeWidth={1.5} aria-hidden="true" />
      </div>
    );
  }

  return (
    <div className={className}>
      <img
        src={imageUrl}
        alt={alt}
        width={600}
        height={450}
        loading={lazy ? 'lazy' : undefined}
        decoding="async"
        onError={() => setFailedUrl(imageUrl)}
      />
    </div>
  );
}
