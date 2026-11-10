import { expect } from '@playwright/test';
import type { BrowseCatalogResponse, CatalogProductDto } from '../../../../driver/port/dtos/BrowseCatalogResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

/** SKUs are aliases, resolved to the values the ERP was given; everything else is a literal as shown. */
export class BrowseCatalogVerification extends ResponseVerification<BrowseCatalogResponse> {
  constructor(response: BrowseCatalogResponse, context: UseCaseContext) {
    super(response, context);
  }

  showsProducts(...skuAliases: string[]): this {
    const expectedSkus = skuAliases.map((alias) => this.getContext().getParamValue(alias));
    expect(this.listedSkus(), 'Catalog products, in order').toEqual(expectedSkus);
    return this;
  }

  showsNoProducts(): this {
    expect(this.listedSkus(), 'Catalog products').toHaveLength(0);
    return this;
  }

  productCount(expectedCount: number): this {
    expect(this.getResponse().productCount, 'Catalog product count').toBe(expectedCount);
    return this;
  }

  product(skuAlias: string, expectedTitle: string, expectedBrand: string, expectedPrice: string): this {
    const sku = this.getContext().getParamValue(skuAlias);
    const product = this.products().find((listed) => listed.sku === sku);
    expect(product, `Catalog should show product '${sku}'`).toBeDefined();
    expect(product?.title, `Title of product '${sku}'`).toBe(expectedTitle);
    expect(product?.brand, `Brand of product '${sku}'`).toBe(expectedBrand);
    expect(product?.price, `Price of product '${sku}'`).toBe(expectedPrice);
    return this;
  }

  categoryOptions(...expectedCategories: string[]): this {
    expect(this.getResponse().categories, 'Category filter options').toEqual(expectedCategories);
    return this;
  }

  selectedCategory(expectedCategory: string): this {
    expect(this.getResponse().selectedCategory, 'Category filter selection').toBe(expectedCategory);
    return this;
  }

  brandOptions(...expectedBrands: string[]): this {
    expect(this.getResponse().brands, 'Brand filter options').toEqual(expectedBrands);
    return this;
  }

  noBrandTicked(): this {
    expect(this.getResponse().tickedBrands, 'Ticked brands').toEqual([]);
    return this;
  }

  sort(expectedSort: string): this {
    expect(this.getResponse().sort, 'Catalog sort').toBe(expectedSort);
    return this;
  }

  message(expectedMessage: string): this {
    expect(this.getResponse().message, 'Catalog message').toBe(expectedMessage);
    return this;
  }

  clearFiltersOffered(): this {
    expect(this.getResponse().clearFiltersOffered, 'Catalog should offer to clear the filters').toBe(true);
    return this;
  }

  private listedSkus(): string[] {
    return this.products().map((product) => product.sku);
  }

  private products(): CatalogProductDto[] {
    expect(this.getResponse(), 'Response should not be null').toBeDefined();
    expect(this.getResponse().products, 'Products list should not be null').toBeDefined();
    return this.getResponse().products;
  }
}
