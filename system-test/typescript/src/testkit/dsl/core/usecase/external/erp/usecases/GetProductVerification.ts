import { expect } from '@playwright/test';
import type { GetProductResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

export class GetProductVerification extends ResponseVerification<GetProductResponse> {
  constructor(response: GetProductResponse, context: UseCaseContext) {
    super(response, context);
  }

  sku(skuParamAlias: string): this {
    const expected = this.getContext().getParamValue(skuParamAlias);
    expect(this.getResponse().sku).toBe(expected);
    return this;
  }

  price(expectedPrice: number): this {
    expect(this.getResponse().price).toBe(expectedPrice);
    return this;
  }

  title(expectedTitle: string): this {
    expect(this.getResponse().title).toBe(expectedTitle);
    return this;
  }

  description(expectedDescription: string): this {
    expect(this.getResponse().description).toBe(expectedDescription);
    return this;
  }

  category(expectedCategory: string): this {
    expect(this.getResponse().category).toBe(expectedCategory);
    return this;
  }

  brand(expectedBrand: string): this {
    expect(this.getResponse().brand).toBe(expectedBrand);
    return this;
  }

  imageUrl(expectedImageUrl: string): this {
    expect(this.getResponse().imageUrl).toBe(expectedImageUrl);
    return this;
  }

  /** Checks the product has an image URL and hands it on, so the caller can fetch the image it points at. */
  async forImageUrl(imageUrlConsumer: (imageUrl: string) => Promise<void>): Promise<this> {
    const imageUrl = this.getResponse().imageUrl;
    expect(imageUrl, `Expected product '${this.getResponse().sku}' to have an image URL, but it had none`).toBeTruthy();
    await imageUrlConsumer(imageUrl ?? '');
    return this;
  }
}
