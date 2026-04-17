export interface ThenCountry extends PromiseLike<void> {
  hasCountry(country: string): this;
  hasTaxRate(taxRate: number): this;
  hasTaxRateIsPositive(): this;
}
