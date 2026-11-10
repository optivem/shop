import { useContext } from 'react';
import { CatalogContext } from './catalog-context';

export function useCatalog() {
  return useContext(CatalogContext);
}
