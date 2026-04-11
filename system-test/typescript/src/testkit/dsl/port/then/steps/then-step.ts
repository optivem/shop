import type { ThenOrder } from './then-order.js';
import type { ThenCoupon } from './then-coupon.js';
import type { ThenClock } from './then-clock.js';
import type { ThenProduct } from './then-product.js';
import type { ThenCountry } from './then-country.js';
import type { ThenBrowseCoupons } from './then-browse-coupons.js';

export interface ThenStep<TThen> extends PromiseLike<void> {
  and(): TThen;
  order(): ThenOrder;
  order(orderNumber: string): ThenOrder;
  coupon(): ThenCoupon;
  coupon(couponCode: string): ThenCoupon;
  clock(): ThenClock;
  product(skuAlias: string): ThenProduct;
  country(countryAlias: string): ThenCountry;
  coupons(): ThenBrowseCoupons;
}
