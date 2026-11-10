/** The catalog as shown. The filter, sort and message fields are UI-only and stay unset on the API channel. */
export interface BrowseCatalogResponse {
  products: CatalogProductDto[];
  productCount?: number;
  /** The category filter's options, "All" included. */
  categories?: string[];
  selectedCategory?: string | null;
  brands?: string[];
  tickedBrands?: string[];
  sort?: string | null;
  message?: string | null;
  clearFiltersOffered?: boolean;
}

export interface CatalogProductDto {
  sku: string;
  title: string;
  brand: string;
  category?: string;
  description?: string;
  /** The price as the channel shows it: "$1,299.99" on the UI, the plain amount on the API. */
  price: string;
}
