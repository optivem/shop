import { expect } from '@playwright/test';
import type { CartLineDto, ViewProductResponse } from '../../../../driver/port/dtos/ViewProductResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
export class ViewProductVerification extends ResponseVerification<ViewProductResponse> {
  constructor(response: ViewProductResponse, context: UseCaseContext) {
    super(response, context);
  }

  title(expectedTitle: string): this {
    expect(this.getResponse().title, 'Product title').toBe(expectedTitle);
    return this;
  }

  brand(expectedBrand: string): this {
    expect(this.getResponse().brand, 'Product brand').toBe(expectedBrand);
    return this;
  }

  category(expectedCategory: string): this {
    expect(this.getResponse().category, 'Product category').toBe(expectedCategory);
    return this;
  }

  sku(expectedSkuAlias: string): this {
    expect(this.getResponse().sku, 'Product SKU').toBe(this.getContext().getParamValue(expectedSkuAlias));
    return this;
  }

  price(expectedPrice: string): this {
    expect(this.getResponse().price, 'Product price').toBe(expectedPrice);
    return this;
  }

  description(expectedDescription: string): this {
    expect(this.getResponse().description, 'Product description').toBe(expectedDescription);
    return this;
  }

  breadcrumbs(...expectedBreadcrumbs: string[]): this {
    expect(this.getResponse().breadcrumbs, 'Breadcrumb trail').toEqual(expectedBreadcrumbs);
    return this;
  }

  quantity(expectedQuantity: number): this {
    expect(this.getResponse().quantity, 'Quantity').toBe(String(expectedQuantity));
    return this;
  }

  message(expectedMessage: string): this {
    expect(this.getResponse().message, 'Product page message').toBe(expectedMessage);
    return this;
  }

  backToCatalogOffered(): this {
    expect(this.getResponse().backToCatalogOffered, 'Product page should offer a link back to the catalog').toBe(true);
    return this;
  }

  addToCartNotOffered(): this {
    expect(this.getResponse().addToCartOffered, 'Product page should not offer to add to the cart').toBe(false);
    return this;
  }

  cartItemCount(expectedCount: number): this {
    expect(this.getResponse().cartItemCount, 'Cart badge').toBe(expectedCount);
    return this;
  }

  onPageOfProduct(skuAlias: string): this {
    expect(this.getResponse().pageSku, 'Product page the visitor is on').toBe(this.getContext().getParamValue(skuAlias));
    return this;
  }

  /** Exactly this line. */
  cartContains(skuAlias: string, expectedQuantity: number): this {
    const expected: CartLineDto = { sku: this.getContext().getParamValue(skuAlias), quantity: expectedQuantity };
    expect(this.cartLines(), 'Cart lines').toEqual([expected]);
    return this;
  }

  cartIsEmpty(): this {
    expect(this.cartLines(), 'Cart lines').toHaveLength(0);
    return this;
  }

  catalogShowsProducts(...skuAliases: string[]): this {
    const expectedSkus = skuAliases.map((alias) => this.getContext().getParamValue(alias));
    expect(this.getResponse().catalogSkus, 'Catalog products, in order').toEqual(expectedSkus);
    return this;
  }

  private cartLines(): CartLineDto[] {
    const lines = this.getResponse().cartLines;
    expect(lines, 'Cart lines should have been read').toBeDefined();
    return lines ?? [];
  }
}
