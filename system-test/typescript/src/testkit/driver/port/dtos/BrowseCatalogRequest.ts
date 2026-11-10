/** Opens the same page address again. */
export const REOPEN_PAGE_ADDRESS = 'reopen-page-address';
/** Clears the filters. */
export const CLEAR_FILTERS = 'clear-filters';
/** Separates the ticked brands. */
export const BRAND_SEPARATOR = ',';

/**
 * How the visitor browses the catalog, applied in this order: follow a header category link, search, pick a
 * category, tick brands, sort; then the follow-up, if any. Unset inputs are left untouched. Only the UI has these
 * controls: the API serves the unfiltered list.
 */
export interface BrowseCatalogRequest {
  headerCategory?: string | null;
  search?: string | null;
  category?: string | null;
  /** The brands to tick, joined with BRAND_SEPARATOR. */
  brands?: string | null;
  /** The sort order's label, e.g. "Price: low to high". */
  sort?: string | null;
  /** REOPEN_PAGE_ADDRESS or CLEAR_FILTERS, run after the other inputs. */
  followUp?: string | null;
}
