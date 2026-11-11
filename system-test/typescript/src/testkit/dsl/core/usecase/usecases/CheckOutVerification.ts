import { expect } from '@playwright/test';
import type {
  CheckOutRejectedLineDto,
  CheckOutResponse,
  CheckOutReviewLineDto,
} from '../../../../driver/port/dtos/CheckOutResponse.js';
import type { CartLineDto } from '../../../../driver/port/dtos/ViewProductResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

/**
 * SKUs are aliases, resolved to the values the ERP was given; messages may name SKU and coupon aliases, which are
 * expanded; everything else is a literal as shown.
 */
export class CheckOutVerification extends ResponseVerification<CheckOutResponse> {
  constructor(response: CheckOutResponse, context: UseCaseContext) {
    super(response, context);
  }

  /** Passes each confirmed order number, in the order shown, to `action`; the numbers must each be different. */
  forEachConfirmedOrder(action: (orderNumber: string) => void): void {
    this.orderNumbers().forEach(action);
  }

  /** The confirmed orders' products (each order's SKU, by order number, in the order shown) are exactly these. */
  placedFor(placedSkus: Map<string, string>, ...expectedSkuAliases: string[]): this {
    expect([...placedSkus.values()], 'Products of the confirmed orders, in order').toEqual(this.skus(expectedSkuAliases));
    return this;
  }

  shownPage(expectedPage: string): this {
    expect(this.shown().shownPage, 'Page the customer ended up on').toBe(expectedPage);
    return this;
  }

  message(expectedMessage: string): this {
    expect(this.shown().message, 'Message shown').toBe(this.getContext().expandAliases(expectedMessage));
    return this;
  }

  /** One of the lines under the error notification's message. */
  messageDetail(expectedDetail: string): this {
    expect(this.shown().messageDetails, 'Lines under the message').toContain(this.getContext().expandAliases(expectedDetail));
    return this;
  }

  /** Exactly these review lines, in this order. */
  reviewLines(...expectedSkuAliases: string[]): this {
    expect(
      this.readReviewLines().map((line) => line.sku),
      'Order review lines, in order',
    ).toEqual(this.skus(expectedSkuAliases));
    return this;
  }

  reviewQuantity(skuAlias: string, expectedQuantity: number): this {
    expect(this.reviewLine(skuAlias).quantity, `Quantity of review line ${skuAlias}`).toBe(expectedQuantity);
    return this;
  }

  reviewLineTotal(skuAlias: string, expectedLineTotal: string): this {
    expect(this.reviewLine(skuAlias).lineTotal, `Line total of review line ${skuAlias}`).toBe(expectedLineTotal);
    return this;
  }

  reviewSubtotal(expectedSubtotal: string): this {
    expect(this.shown().reviewSubtotal, 'Order review subtotal').toBe(expectedSubtotal);
    return this;
  }

  openedTax(expectedCountry: string, expectedRate: string): this {
    expect(this.shown().openedTaxCountry, 'Tax country when the checkout opened').toBe(expectedCountry);
    expect(this.shown().openedEstimatedTaxRate, 'Estimated tax rate when the checkout opened').toBe(expectedRate);
    return this;
  }

  tax(expectedCountry: string, expectedRate: string): this {
    expect(this.shown().taxCountry, 'Tax country').toBe(expectedCountry);
    expect(this.shown().estimatedTaxRate, 'Estimated tax rate').toBe(expectedRate);
    return this;
  }

  reviewNote(expectedNote: string): this {
    expect(this.shown().reviewNote, 'Order review note').toBe(expectedNote);
    return this;
  }

  savedAddressSelected(expectedAddress: string): this {
    expect(this.shown().savedAddress, 'Saved address shown').toBe(expectedAddress);
    expect(this.shown().savedAddressSelected, 'Saved address should be selected').toBe(true);
    return this;
  }

  thanks(expectedThanks: string): this {
    expect(this.shown().thanks, 'Confirmation heading').toBe(expectedThanks);
    return this;
  }

  /** The confirmed order numbers, in the order shown; each distinct. */
  private orderNumbers(): string[] {
    const orders = this.shown().confirmedOrders;
    expect(orders, 'Confirmed orders should have been read').toBeDefined();
    const numbers = (orders ?? []).map((order) => order.orderNumber);
    expect(new Set(numbers).size, `Confirmed order numbers should each be different: ${JSON.stringify(numbers)}`).toBe(numbers.length);
    return numbers;
  }

  noOrderConfirmed(): this {
    expect(this.orderNumbers(), 'Confirmed orders').toEqual([]);
    return this;
  }

  eachOrderLinksToItsDetails(): this {
    const orders = this.shown().confirmedOrders ?? [];
    expect(orders.length, 'Confirmed orders').toBeGreaterThan(0);
    for (const order of orders) {
      const link = order.link ?? '';
      expect(link, `Link of confirmed order ${order.orderNumber}`).toContain('/order-details');
      expect(link, `Link of confirmed order ${order.orderNumber}`).toContain(encodeURIComponent(order.orderNumber));
    }
    return this;
  }

  viewMyOrdersAndContinueShoppingOffered(): this {
    expect(this.shown().viewMyOrdersOffered, 'Confirmation should offer "View my orders"').toBe(true);
    expect(this.shown().continueShoppingOffered, 'Confirmation should offer "Continue shopping"').toBe(true);
    return this;
  }

  rejectedLine(skuAlias: string, expectedReason: string): this {
    const sku = this.getContext().getParamValue(skuAlias);
    const line = this.readRejectedLines().find((rejected) => rejected.sku === sku);
    if (!line) {
      throw new Error(`The confirmation shows no rejected line ${skuAlias}; it shows ${JSON.stringify(this.readRejectedLines())}`);
    }
    expect(line.reason, `Why ${skuAlias} was rejected`).toBe(this.getContext().expandAliases(expectedReason));
    return this;
  }

  noRejectedLines(): this {
    expect(this.readRejectedLines(), 'Rejected lines').toEqual([]);
    return this;
  }

  /** Exactly these cart lines, in this order; none for an empty cart. */
  cartLines(...expectedSkuAliases: string[]): this {
    expect(
      this.cart().map((line) => line.sku),
      'Cart lines after checkout, in order',
    ).toEqual(this.skus(expectedSkuAliases));
    return this;
  }

  cartLineQuantity(skuAlias: string, expectedQuantity: number): this {
    const sku = this.getContext().getParamValue(skuAlias);
    const line = this.cart().find((cartLine) => cartLine.sku === sku);
    if (!line) {
      throw new Error(`The cart has no line ${skuAlias}; it has ${JSON.stringify(this.cart())}`);
    }
    expect(line.quantity, `Quantity of cart line ${skuAlias}`).toBe(expectedQuantity);
    return this;
  }

  private skus(skuAliases: string[]): string[] {
    return skuAliases.map((alias) => this.getContext().getParamValue(alias));
  }

  private reviewLine(skuAlias: string): CheckOutReviewLineDto {
    const sku = this.getContext().getParamValue(skuAlias);
    const line = this.readReviewLines().find((reviewLine) => reviewLine.sku === sku);
    if (!line) {
      throw new Error(`The order review has no line ${skuAlias}; it has ${JSON.stringify(this.readReviewLines())}`);
    }
    return line;
  }

  private readReviewLines(): CheckOutReviewLineDto[] {
    const lines = this.shown().reviewLines;
    expect(lines, 'Order review should have been read').toBeDefined();
    return lines ?? [];
  }

  private readRejectedLines(): CheckOutRejectedLineDto[] {
    const lines = this.shown().rejectedLines;
    expect(lines, 'Rejected lines should have been read').toBeDefined();
    return lines ?? [];
  }

  private cart(): CartLineDto[] {
    const lines = this.shown().cartLines;
    expect(lines, 'Cart should have been read after placing the order').toBeDefined();
    return lines ?? [];
  }

  private shown(): CheckOutResponse {
    const response = this.getResponse();
    expect(response, 'Response should not be null').toBeDefined();
    return response;
  }
}
