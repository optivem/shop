import { expect } from '@playwright/test';
import type { ManageCartLineDto, ManageCartResponse } from '../../../../driver/port/dtos/ManageCartResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
export class ManageCartVerification extends ResponseVerification<ManageCartResponse> {
  constructor(response: ManageCartResponse, context: UseCaseContext) {
    super(response, context);
  }

  message(expectedMessage: string): this {
    expect(this.getResponse().message, 'Message shown').toBe(expectedMessage);
    return this;
  }

  cartItemCount(expectedCount: number): this {
    expect(this.getResponse().cartItemCount, 'Cart badge').toBe(expectedCount);
    return this;
  }

  noCartBadge(): this {
    expect(this.getResponse().cartItemCount, 'Cart badge should be hidden').toBe(0);
    return this;
  }

  /** Exactly these lines, in this order. */
  lines(...expectedSkuAliases: string[]): this {
    const expectedSkus = expectedSkuAliases.map((alias) => this.getContext().getParamValue(alias));
    expect(
      this.cartLines().map((line) => line.sku),
      'Cart lines, in order',
    ).toEqual(expectedSkus);
    return this;
  }

  line(skuAlias: string, expectedTitle: string, expectedUnitPrice: string, expectedQuantity: number, expectedLineTotal: string): this {
    expect(this.findLine(skuAlias).title, `Title of cart line ${skuAlias}`).toBe(expectedTitle);
    return this.linePrices(skuAlias, expectedUnitPrice, expectedLineTotal).lineQuantity(skuAlias, expectedQuantity);
  }

  lineQuantity(skuAlias: string, expectedQuantity: number): this {
    expect(this.findLine(skuAlias).quantity, `Quantity of cart line ${skuAlias}`).toBe(expectedQuantity);
    return this;
  }

  linePrices(skuAlias: string, expectedUnitPrice: string, expectedLineTotal: string): this {
    expect(this.findLine(skuAlias).unitPrice, `Unit price of cart line ${skuAlias}`).toBe(expectedUnitPrice);
    return this.lineTotal(skuAlias, expectedLineTotal);
  }

  lineTotal(skuAlias: string, expectedLineTotal: string): this {
    expect(this.findLine(skuAlias).lineTotal, `Line total of cart line ${skuAlias}`).toBe(expectedLineTotal);
    return this;
  }

  itemCount(expectedCount: number): this {
    expect(this.getResponse().itemCount, 'Number of items the cart shows').toBe(expectedCount);
    return this;
  }

  subtotal(expectedSubtotal: string): this {
    expect(this.getResponse().subtotal, 'Cart subtotal').toBe(expectedSubtotal);
    return this;
  }

  checkoutNote(expectedNote: string): this {
    expect(this.getResponse().checkoutNote, 'Cart note about checkout').toBe(expectedNote);
    return this;
  }

  startShoppingOffered(): this {
    expect(this.getResponse().startShoppingOffered, 'Cart should offer a link to start shopping').toBe(true);
    return this;
  }

  notAskedToSignIn(): this {
    expect(this.getResponse().askedToSignIn, 'Visitor should not be asked to sign in').toBe(false);
    return this;
  }

  private findLine(skuAlias: string): ManageCartLineDto {
    const sku = this.getContext().getParamValue(skuAlias);
    const line = this.cartLines().find((cartLine) => cartLine.sku === sku);
    if (!line) {
      throw new Error(`The cart has no line ${skuAlias}; it has ${JSON.stringify(this.cartLines())}`);
    }
    return line;
  }

  private cartLines(): ManageCartLineDto[] {
    const lines = this.getResponse().lines;
    expect(lines, 'Cart lines should have been read').toBeDefined();
    return lines ?? [];
  }
}
