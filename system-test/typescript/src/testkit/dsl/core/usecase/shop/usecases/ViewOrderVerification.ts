import { expect } from '@playwright/test';
import type { ViewOrderResponse } from '../../../../../driver/port/shop/dtos/ViewOrderResponse.js';
import { ResponseVerification } from '../../../shared/response-verification.js';
import type { UseCaseContext } from '../../../shared/use-case-context.js';

export class ViewOrderVerification extends ResponseVerification<ViewOrderResponse> {
  constructor(response: ViewOrderResponse, context: UseCaseContext) {
    super(response, context);
  }

  orderNumber(aliasOrValue: string): this {
    const expected = this.getContext().getResultValue(aliasOrValue);
    expect(this.getResponse().orderNumber).toBe(expected);
    return this;
  }

  sku(skuAlias: string): this {
    const expected = this.getContext().getParamValue(skuAlias);
    expect(this.getResponse().sku).toBe(expected);
    return this;
  }

  quantity(quantity: number): this {
    expect(this.getResponse().quantity).toBe(quantity);
    return this;
  }

  unitPrice(unitPrice: number): this {
    expect(this.getResponse().unitPrice).toBe(unitPrice);
    return this;
  }

  basePrice(basePrice: number): this {
    expect(this.getResponse().basePrice).toBe(basePrice);
    return this;
  }

  totalPrice(totalPrice: number): this {
    expect(this.getResponse().totalPrice).toBe(totalPrice);
    return this;
  }

  totalPriceGreaterThanZero(): this {
    expect(this.getResponse().totalPrice).toBeGreaterThan(0);
    return this;
  }

  status(status: string): this {
    expect(this.getResponse().status).toBe(status);
    return this;
  }

  country(countryAlias: string): this {
    const expected = this.getContext().getParamValueOrLiteral(countryAlias);
    expect(this.getResponse().country).toBe(expected);
    return this;
  }
}
