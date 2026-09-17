import type { Result } from '../../common/result.js';
import type { AsyncCloseable } from './async-closeable.js';
import type { GoToMyShopRequest } from './dtos/GoToMyShopRequest.js';
import type { GoToMyShopResponse } from './dtos/GoToMyShopResponse.js';
import type { PlaceOrderRequest } from './dtos/PlaceOrderRequest.js';
import type { PlaceOrderResponse } from './dtos/PlaceOrderResponse.js';
import type { CancelOrderRequest } from './dtos/CancelOrderRequest.js';
import type { CancelOrderResponse } from './dtos/CancelOrderResponse.js';
import type { DeliverOrderRequest } from './dtos/DeliverOrderRequest.js';
import type { DeliverOrderResponse } from './dtos/DeliverOrderResponse.js';
import type { ViewOrderRequest } from './dtos/ViewOrderRequest.js';
import type { ViewOrderResponse } from './dtos/ViewOrderResponse.js';
import type { SystemError } from './dtos/errors/SystemError.js';
import type { PublishCouponRequest } from './dtos/PublishCouponRequest.js';
import type { PublishCouponResponse } from './dtos/PublishCouponResponse.js';
import type { BrowseCouponsRequest } from './dtos/BrowseCouponsRequest.js';
import type { BrowseCouponsResponse } from './dtos/BrowseCouponsResponse.js';

export interface MyShopDriver extends AsyncCloseable {
  goToMyShop(request: GoToMyShopRequest): Promise<Result<GoToMyShopResponse, SystemError>>;
  placeOrder(request: PlaceOrderRequest): Promise<Result<PlaceOrderResponse, SystemError>>;
  cancelOrder(request: CancelOrderRequest): Promise<Result<CancelOrderResponse, SystemError>>;
  deliverOrder(request: DeliverOrderRequest): Promise<Result<DeliverOrderResponse, SystemError>>;
  viewOrder(request: ViewOrderRequest): Promise<Result<ViewOrderResponse, SystemError>>;
  publishCoupon(request: PublishCouponRequest): Promise<Result<PublishCouponResponse, SystemError>>;
  browseCoupons(request: BrowseCouponsRequest): Promise<Result<BrowseCouponsResponse, SystemError>>;
}
