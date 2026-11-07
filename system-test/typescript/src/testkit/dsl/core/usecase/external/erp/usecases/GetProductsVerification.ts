import { expect } from '@playwright/test';
import type { GetProductResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductResponse.js';
import type { GetProductsResponse } from '../../../../../../driver/port/external/erp/dtos/GetProductsResponse.js';
import { ResponseVerification } from '../../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../../shared/use-case-context.js';

/**
 * Asserts on the products a scenario staged, not on the list's size: the real ERP also lists
 * products seeded or created by other tests.
 */
export class GetProductsVerification extends ResponseVerification<GetProductsResponse> {
  constructor(response: GetProductsResponse, context: UseCaseContext) {
    super(response, context);
  }

  containsProduct(skuParamAlias: string, expectedPrice: number | string): this {
    const expectedSku = this.getContext().getParamValue(skuParamAlias);
    const product = this.findProduct(expectedSku);
    expect(product.price, `Expected product '${expectedSku}' to have price ${expectedPrice}`).toBe(Number(expectedPrice));
    return this;
  }

  private findProduct(sku: string): GetProductResponse {
    const products = this.getResponse().products;
    const product = products.find((p) => p.sku === sku);
    if (!product) {
      throw new Error(`Expected product list to contain SKU '${sku}', but it contained [${products.map((p) => p.sku).join(', ')}]`);
    }
    return product;
  }
}
