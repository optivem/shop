import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';
import type { GivenStage } from '../given-stage.js';
import type { WhenStage } from '../../when/when-stage.js';
import type { ThenStage } from '../../then/then-stage.js';

export interface GivenOrder {
  withOrderNumber(orderNumber: string): GivenOrder;
  withSku(sku: string): GivenOrder;
  withQuantity(quantity: string | number): GivenOrder;
  withCountry(country: string): GivenOrder;
  withCouponCode(couponCode: string | null): GivenOrder;
  withIdempotencyKey(idempotencyKey: string): GivenOrder;
  /** The order is placed by the customer known in this scenario by the given alias, or by the default customer (customer1) when no alias is given. */
  placedByCustomer(alias?: string): GivenOrder;
  /** The order is placed at this time (ISO-8601) by setting the clock just before; the scenario must run isolated. */
  placedAt(orderTimestamp: string): GivenOrder;
  withStatus(status: OrderStatus): GivenOrder;
  and(): GivenStage;
  when(): WhenStage;
  then(): ThenStage;
}
