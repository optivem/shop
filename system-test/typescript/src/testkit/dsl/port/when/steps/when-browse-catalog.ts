import type { ThenCatalogResultStage } from '../../then/steps/then-catalog.js';

/**
 * Browses the catalog as a visitor (or as the logged-in user). The steps run in this order: follow a header
 * category link, search, pick a category, tick brands, sort; then reopen the page address or clear the filters.
 */
export interface WhenBrowseCatalog {
  withHeaderCategoryLink(category: string): this;
  withSearch(term: string): this;
  withCategory(category: string): this;
  withBrands(...brands: string[]): this;
  /** The sort order's label, e.g. "Price: high to low". */
  withSort(sort: string): this;
  /** After the other steps, opens the same page address again. */
  withPageAddressReopened(): this;
  /** After the other steps, clears the filters. */
  withFiltersCleared(): this;
  then(): ThenCatalogResultStage;
}
