import type { GivenClock } from './steps/given-clock.js';
import type { GivenErp } from './steps/given-erp.js';
import type { GivenProduct } from './steps/given-product.js';
import type { GivenProducts } from './steps/given-products.js';
import type { GivenPromotion } from './steps/given-promotion.js';
import type { GivenCoupon } from './steps/given-coupon.js';
import type { GivenCountry } from './steps/given-country.js';
import type { GivenOrder } from './steps/given-order.js';
import type { GivenProfile } from './steps/given-profile.js';
import type { WhenStage } from '../when/when-stage.js';
import type { ThenStage } from '../then/then-stage.js';

export interface GivenStage {
  clock(): GivenClock;
  erp(): GivenErp;
  product(): GivenProduct;
  /** Stages the ERP product list; it replaces the default product, so a list scenario sees only what it stages. */
  products(): GivenProducts;
  promotion(): GivenPromotion;
  coupon(): GivenCoupon;
  country(): GivenCountry;
  order(): GivenOrder;
  profile(): GivenProfile;
  /** Logs in as the customer known in this scenario by the given alias, or as the default customer (customer1) when no alias is given. */
  loggedInAsCustomer(alias?: string): GivenStage;
  loggedInAsAdmin(): GivenStage;
  notLoggedIn(): GivenStage;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
