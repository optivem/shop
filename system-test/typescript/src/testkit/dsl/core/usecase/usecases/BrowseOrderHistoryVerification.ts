import { expect } from '@playwright/test';
import type { BrowseOrderHistoryResponse } from '../../../../driver/port/dtos/BrowseOrderHistoryResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

export class BrowseOrderHistoryVerification extends ResponseVerification<BrowseOrderHistoryResponse> {
  constructor(response: BrowseOrderHistoryResponse, context: UseCaseContext) {
    super(response, context);
  }

  containsOrder(orderNumberResultAlias: string): this {
    const orderNumber = this.getContext().getResultValue(orderNumberResultAlias);
    expect(this.listedOrderNumbers(), `Order history should contain order '${orderNumber}'`).toContain(orderNumber);
    return this;
  }

  doesNotContainOrder(orderNumberResultAlias: string): this {
    const orderNumber = this.getContext().getResultValue(orderNumberResultAlias);
    expect(this.listedOrderNumbers(), `Order history should not contain order '${orderNumber}'`).not.toContain(orderNumber);
    return this;
  }

  private listedOrderNumbers(): string[] {
    expect(this.getResponse(), 'Response should not be null').not.toBeNull();
    expect(this.getResponse().orders, 'Orders list should not be null').toBeDefined();
    return this.getResponse().orders.map((order) => order.orderNumber);
  }
}
