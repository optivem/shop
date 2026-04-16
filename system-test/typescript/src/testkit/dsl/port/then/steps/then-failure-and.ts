import type { ThenOrder } from './then-order.js';
import type { ThenCoupon } from './then-coupon.js';

export interface ThenFailureAnd {
  order(orderNumber?: string): ThenOrder;
  coupon(couponCode?: string): ThenCoupon;
}
