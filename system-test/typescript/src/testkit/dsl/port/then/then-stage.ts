import type { ThenClock } from './steps/then-clock.js';
import type { ThenProduct } from './steps/then-product.js';
import type { ThenProducts } from './steps/then-products.js';
import type { ThenCountry } from './steps/then-country.js';

export interface ThenStage {
  clock(): ThenClock;
  product(skuAlias: string): ThenProduct;
  products(): ThenProducts;
  country(countryAlias: string): ThenCountry;
}
