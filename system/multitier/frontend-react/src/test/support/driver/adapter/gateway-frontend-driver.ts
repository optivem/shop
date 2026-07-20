// Gateway Frontend Driver — realizes the Frontend DSL by calling the service
// gateways directly (no React render) and asserting on the returned Result.
// Used by the integration/latest specs. It reads the same DSL lines as the UI
// driver; only the realization differs — screen text there, a Result here.
import { expect } from 'vitest';
import { OrderGateway } from '../../../../services/order-service';
import { browseCoupons, createCoupon } from '../../../../services/coupon-service';
import { routeApiTo } from '../../../test-utils';
import type { Result } from '../../../../types/result.types';
import type {
  PlaceOrderResponse,
  BrowseOrderHistoryResponse,
  BrowseCouponsResponse,
} from '../../../../types/api.types';
import type { FrontendDriver } from '../port/frontend-driver';
import type { PlaceOrderGesture } from '../port/dtos/PlaceOrderGesture';
import type { OrderHistoryRowExpectation } from '../port/dtos/OrderHistoryRowExpectation';

export class GatewayFrontendDriver implements FrontendDriver {
  private baseUrl = '';
  private lastPlaceOrder?: Result<PlaceOrderResponse>;
  private lastHistory?: Result<BrowseOrderHistoryResponse>;
  private lastCoupons?: Result<BrowseCouponsResponse>;
  private lastPublish?: Result<void>;

  useBackend(baseUrl: string): void {
    this.baseUrl = baseUrl;
    // The coupon service calls relative /api/coupons; point it at the mock server.
    routeApiTo(baseUrl);
  }

  private orders(): OrderGateway {
    return new OrderGateway(`${this.baseUrl}/api/orders`);
  }

  async placeOrder(gesture: PlaceOrderGesture): Promise<void> {
    this.lastPlaceOrder = await this.orders().placeOrder(
      gesture.sku,
      Number(gesture.quantity),
      gesture.country,
      gesture.couponCode,
    );
  }

  async hasConfirmation(orderNumber: string): Promise<void> {
    const result = this.lastPlaceOrder;
    expect(result?.success).toBe(true);
    if (result?.success) {
      expect(result.data.orderNumber).toBe(orderNumber);
    }
  }

  async hasError(_message: string): Promise<void> {
    expect(this.lastPlaceOrder?.success).toBe(false);
  }

  // The gateway's job is to turn the 422's ProblemDetail into ApiError.fieldErrors. The UI
  // driver checks it reaches the screen; here we check it survives the parse.
  async hasFieldError(field: string, message: string): Promise<void> {
    const result = this.lastPlaceOrder;
    expect(result?.success).toBe(false);
    if (result && !result.success) {
      expect(result.error.fieldErrors).toContain(`${field}: ${message}`);
    }
  }

  async browseOrderHistory(): Promise<void> {
    this.lastHistory = await this.orders().browseOrderHistory();
  }

  async showsOrder(orderNumber: string, expected?: OrderHistoryRowExpectation): Promise<void> {
    const result = this.lastHistory;
    expect(result?.success).toBe(true);
    if (result?.success) {
      const order = result.data.orders.find((o) => o.orderNumber === orderNumber);
      expect(order).toBeDefined();
      if (expected?.totalPrice !== undefined) {
        expect(order?.totalPrice).toBe(expected.totalPrice);
      }
      if (expected?.status !== undefined) {
        expect(order?.status).toBe(expected.status);
      }
    }
  }

  async browseCoupons(): Promise<void> {
    this.lastCoupons = await browseCoupons();
  }

  async showsCoupon(code: string): Promise<void> {
    const result = this.lastCoupons;
    expect(result?.success).toBe(true);
    if (result?.success) {
      expect(result.data.coupons.some((coupon) => coupon.code === code)).toBe(true);
    }
  }

  async publishCoupon(code: string, discountRate: number): Promise<void> {
    this.lastPublish = await createCoupon(code, discountRate, null, null, null);
  }

  async succeeded(): Promise<void> {
    expect(this.lastPublish?.success).toBe(true);
  }

  viewOrderDetails(): Promise<void> {
    return this.notAtGatewayLevel('viewOrderDetails');
  }

  showsOrderDetails(): Promise<void> {
    return this.notAtGatewayLevel('showsOrderDetails');
  }

  showsCancelAndDeliverActions(): Promise<void> {
    return this.notAtGatewayLevel('showsCancelAndDeliverActions');
  }

  hidesCancelAndDeliverActions(): Promise<void> {
    return this.notAtGatewayLevel('hidesCancelAndDeliverActions');
  }

  showsNotFound(): Promise<void> {
    return this.notAtGatewayLevel('showsNotFound');
  }

  // Cancelling is reached from the order-details screen, which this level does not render.
  cancelOrder(): Promise<void> {
    return this.notAtGatewayLevel('cancelOrder');
  }

  wasCancelled(): Promise<void> {
    return this.notAtGatewayLevel('wasCancelled');
  }

  cancelWasRejected(): Promise<void> {
    return this.notAtGatewayLevel('cancelWasRejected');
  }

  private notAtGatewayLevel(op: string): never {
    throw new Error(`FrontendDsl.${op} is not exercised at the narrow-integration (gateway) level`);
  }
}
