import type { WhenPlaceOrder } from './steps/when-place-order.js';
import type { WhenCancelOrder } from './steps/when-cancel-order.js';
import type { WhenDeliverOrder } from './steps/when-deliver-order.js';
import type { WhenBrowseOrderHistory } from './steps/when-browse-order-history.js';
import type { WhenViewOrder } from './steps/when-view-order.js';
import type { WhenPublishCoupon } from './steps/when-publish-coupon.js';
import type { WhenBrowseCoupons } from './steps/when-browse-coupons.js';
import type { WhenViewProfile } from './steps/when-view-profile.js';
import type { WhenUpdateProfile } from './steps/when-update-profile.js';
import type { WhenBrowseCatalog } from './steps/when-browse-catalog.js';
import type { WhenViewProduct } from './steps/when-view-product.js';
import type { WhenManageCart } from './steps/when-manage-cart.js';
import type { WhenCheckOut } from './steps/when-check-out.js';

export interface WhenStage {
  placeOrder(): WhenPlaceOrder;
  cancelOrder(): WhenCancelOrder;
  deliverOrder(): WhenDeliverOrder;
  viewOrder(): WhenViewOrder;
  browseOrderHistory(): WhenBrowseOrderHistory;
  publishCoupon(): WhenPublishCoupon;
  browseCoupons(): WhenBrowseCoupons;
  viewProfile(): WhenViewProfile;
  updateProfile(): WhenUpdateProfile;
  browseCatalog(): WhenBrowseCatalog;
  viewProduct(): WhenViewProduct;
  manageCart(): WhenManageCart;
  checkOut(): WhenCheckOut;
}
