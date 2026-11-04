import type { OrderStatus } from '../../../../common/domain/OrderStatus.js';
export interface ThenOrder extends PromiseLike<void> {
  and(): this;
  hasSku(expectedSku: string): this;
  hasQuantity(expectedQuantity: number): this;
  hasUnitPrice(expectedUnitPrice: number): this;
  hasBasePrice(basePrice: string | number): this;
  hasSubtotalPrice(subtotalPrice: string | number): this;
  hasTotalPrice(totalPrice: string | number): this;
  hasStatus(expectedStatus: OrderStatus): this;
  hasTotalPriceGreaterThanZero(): this;
  hasOrderNumberPrefix(expectedPrefix: string): this;
  /** The order is the one known in this scenario by the given order-number alias. */
  hasOrderNumber(orderNumber: string): this;
  /** The order is not the one known in this scenario by the given order-number alias. */
  doesNotHaveOrderNumber(orderNumber: string): this;
  hasDiscountRate(expectedDiscountRate: number): this;
  hasDiscountAmount(expectedDiscountAmount: string | number): this;
  hasAppliedCoupon(expectedCouponCode?: string | null): this;
  hasTaxRate(expectedTaxRate: string | number): this;
  hasTaxAmount(expectedTaxAmount: string | number): this;
}
