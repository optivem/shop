import type { GivenClock } from './steps/given-clock.js';
import type { GivenProduct } from './steps/given-product.js';
import type { GivenPromotion } from './steps/given-promotion.js';
import type { GivenCoupon } from './steps/given-coupon.js';
import type { GivenCountry } from './steps/given-country.js';
import type { GivenOrder } from './steps/given-order.js';
import type { WhenStage } from '../when/when-stage.js';
import type { ThenStage } from '../then/then-stage.js';

export interface GivenStage {
  clock(): GivenClock;
  product(): GivenProduct;
  promotion(): GivenPromotion;
  coupon(): GivenCoupon;
  country(): GivenCountry;
  order(): GivenOrder;
  when(): WhenStage;
  then(): ThenStage;
}
