export interface ThenCatalogResultStage {
  shouldSucceed(): ThenCatalogSuccess;
  shouldFail(): ThenCatalogFailure;
}

export interface ThenCatalogSuccess extends PromiseLike<void> {
  and(): this;
  /** The catalog the executed browse showed. */
  catalog(): ThenCatalog;
}

export interface ThenCatalogFailure extends PromiseLike<void> {
  and(): this;
  errorMessage(expectedMessage: string): this;
}

/** The catalog the executed browse showed. SKUs are aliases; titles, brands, prices and labels are literals as shown. */
export interface ThenCatalog extends PromiseLike<void> {
  and(): this;
  /** Exactly these products, in this order. */
  showsProducts(...skuAliases: string[]): this;
  showsNoProducts(): this;
  hasProductCount(expectedCount: number): this;
  showsProduct(skuAlias: string, expectedTitle: string, expectedBrand: string, expectedPrice: string): this;
  /** The category filter's options, "All" included, in order. */
  offersCategories(...expectedCategories: string[]): this;
  hasCategorySelected(expectedCategory: string): this;
  offersBrands(...expectedBrands: string[]): this;
  hasNoBrandTicked(): this;
  hasSort(expectedSort: string): this;
  showsMessage(expectedMessage: string): this;
  offersToClearFilters(): this;
  /** The rest of the site keeps working: the acting user can still open their order history. */
  canOpenMyOrders(): this;
}
