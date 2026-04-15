import type { WhenPlaceOrder } from './steps/when-place-order.js';
import type { WhenCancelOrder } from './steps/when-cancel-order.js';
import type { WhenViewOrder } from './steps/when-view-order.js';
import type { WhenPublishCoupon } from './steps/when-publish-coupon.js';
import type { WhenBrowseCoupons } from './steps/when-browse-coupons.js';

export interface WhenStage {
  placeOrder(): WhenPlaceOrder;
  cancelOrder(): WhenCancelOrder;
  viewOrder(): WhenViewOrder;
  publishCoupon(): WhenPublishCoupon;
  browseCoupons(): WhenBrowseCoupons;
}
