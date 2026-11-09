import { expect } from '@playwright/test';
import type { ViewOrderResponse } from '../../../../driver/port/dtos/ViewOrderResponse.js';
import type { ShippingAddress } from '../../../../driver/port/dtos/ShippingAddress.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import { Converter } from '../../../../common/converter.js';

export class ViewOrderVerification extends ResponseVerification<ViewOrderResponse> {
  constructor(response: ViewOrderResponse, context: UseCaseContext) {
    super(response, context);
  }

  orderNumber(aliasOrValue: string): this {
    const expected = this.getContext().getResultValue(aliasOrValue);
    expect(this.getResponse().orderNumber).toBe(expected);
    return this;
  }

  orderNumberIsNot(aliasOrValue: string): this {
    const other = this.getContext().getResultValue(aliasOrValue);
    expect(this.getResponse().orderNumber, `Expected order number to differ from '${other}'`).not.toBe(other);
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

  unitPrice(unitPrice: string | number): this {
    expect(Converter.normalizeDecimalOrNull(this.getResponse().unitPrice)).toBe(Converter.normalizeDecimal(unitPrice));
    return this;
  }

  basePrice(basePrice: string | number): this {
    expect(Converter.normalizeDecimalOrNull(this.getResponse().basePrice)).toBe(Converter.normalizeDecimal(basePrice));
    return this;
  }

  totalPrice(totalPrice: string | number): this {
    expect(Converter.normalizeDecimalOrNull(this.getResponse().totalPrice)).toBe(Converter.normalizeDecimal(totalPrice));
    return this;
  }

  totalPriceGreaterThanZero(): this {
    expect(Number(this.getResponse().totalPrice)).toBeGreaterThan(0);
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

  /** The address's country is an alias, resolved like country(). */
  shippingAddress(expected: ShippingAddress): this {
    expect(this.getResponse().shippingAddress, 'Order shipping address').toEqual({
      ...expected,
      country: this.getContext().getParamValueOrLiteral(expected.country),
    });
    return this;
  }

  noShippingAddress(): this {
    expect(this.getResponse().shippingAddress ?? null, 'Order shipping address').toBeNull();
    return this;
  }
}
