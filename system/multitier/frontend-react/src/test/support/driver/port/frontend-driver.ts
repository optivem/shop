// The seam both drivers implement. Gestures drive; the matching query methods
// assert the outcome. useBackend points the driver at the stubbed backend — the
// harness calls it, never a spec (routeApiTo for the UI, base URL for the gateway).
// Some interactions only exist at one level — order details, its status-gated
// actions, and cancelling from that screen are UI-only — so the driver that can't
// realize a verb throws a clearly-labelled error. The latest specs never call an
// unsupported verb, so those throws stay dormant and simply document the level
// boundary.
import type { PlaceOrderGesture } from './dtos/PlaceOrderGesture';
import type { OrderDetailExpectation } from './dtos/OrderDetailExpectation';
import type { OrderHistoryRowExpectation } from './dtos/OrderHistoryRowExpectation';

export interface FrontendDriver {
  useBackend(baseUrl: string): void;

  // place order (both levels)
  placeOrder(gesture: PlaceOrderGesture): Promise<void>;
  hasConfirmation(orderNumber: string): Promise<void>;
  hasError(message: string): Promise<void>;
  hasFieldError(field: string, message: string): Promise<void>;

  // browse order history (both levels)
  browseOrderHistory(): Promise<void>;
  showsOrder(orderNumber: string, expected?: OrderHistoryRowExpectation): Promise<void>;

  // browse coupons (both levels)
  browseCoupons(): Promise<void>;
  showsCoupon(code: string): Promise<void>;

  // view order details (UI only — asserts on the rendered screen)
  viewOrderDetails(orderNumber: string): Promise<void>;
  showsOrderDetails(orderNumber: string, expected: OrderDetailExpectation): Promise<void>;
  showsCancelAndDeliverActions(): Promise<void>;
  hidesCancelAndDeliverActions(): Promise<void>;
  showsNotFound(): Promise<void>;

  // cancel order — reached from the order-details screen, so UI only
  cancelOrder(orderNumber: string): Promise<void>;
  wasCancelled(): Promise<void>;
  cancelWasRejected(message: string): Promise<void>;

  // publish coupon (both levels)
  publishCoupon(code: string, discountRate: number): Promise<void>;
  succeeded(): Promise<void>;
}
