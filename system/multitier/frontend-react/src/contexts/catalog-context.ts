import { createContext } from 'react';
import type { Product } from '../types/api.types';

export interface CatalogContextType {
  products: Product[];
  isLoading: boolean;
  /** Set when the ERP (or the shop API) could not deliver the catalog. */
  error: string | null;
  /** Reads the catalog again, e.g. so the cart shows current prices. */
  reload: () => void;
}

/** Without a provider (isolated page tests) there is no catalog and nothing is fetched. */
export const CatalogContext = createContext<CatalogContextType>({
  products: [],
  isLoading: false,
  error: null,
  reload: () => undefined,
});
