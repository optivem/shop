import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { productService } from '../services/product-service';
import type { Product } from '../types/api.types';
import { CatalogContext } from './catalog-context';

/** Loads the public catalog once for the header's category links and the catalog, cart and checkout pages. */
export function CatalogProvider({ children }: Readonly<{ children: ReactNode }>) {
  const [products, setProducts] = useState<Product[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [reloadCount, setReloadCount] = useState(0);
  const [loadedCount, setLoadedCount] = useState(-1);

  useEffect(() => {
    let ignore = false;
    void productService.browseProducts().then((result) => {
      if (ignore) return;
      if (result.success) {
        setProducts(result.data);
        setError(null);
      } else {
        setError(result.error.message);
      }
      setLoadedCount(reloadCount);
    });
    return () => {
      ignore = true;
    };
  }, [reloadCount]);

  const reload = useCallback(() => setReloadCount((count) => count + 1), []);

  const value = useMemo(
    () => ({ products, error, isLoading: loadedCount !== reloadCount, reload }),
    [products, error, loadedCount, reloadCount, reload],
  );

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>;
}
