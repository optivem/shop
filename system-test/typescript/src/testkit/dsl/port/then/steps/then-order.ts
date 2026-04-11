import type { ThenStep } from './then-step.js';

export interface ThenOrder extends ThenStep<ThenOrder> {
  hasSku(expectedSku: string): ThenOrder;
  hasQuantity(expectedQuantity: number): ThenOrder;
  hasUnitPrice(expectedUnitPrice: number): ThenOrder;
  hasBasePrice(expectedBasePrice: string | number): ThenOrder;
  hasSubtotalPrice(expectedSubtotalPrice: string | number): ThenOrder;
  hasTotalPrice(expectedTotalPrice: string | number): ThenOrder;
  hasStatus(expectedStatus: string): ThenOrder;
  hasTotalPriceGreaterThanZero(): ThenOrder;
  hasOrderNumberPrefix(expectedPrefix: string): ThenOrder;
  hasDiscountRate(expectedDiscountRate: number): ThenOrder;
  hasDiscountAmount(expectedDiscountAmount: string | number): ThenOrder;
  hasAppliedCouponCode(expectedCouponCode: string): ThenOrder;
  hasAppliedCoupon(expectedCouponCode?: string): ThenOrder;
  hasTaxRate(expectedTaxRate: string | number): ThenOrder;
  hasTaxAmount(expectedTaxAmount: string): ThenOrder;
}
