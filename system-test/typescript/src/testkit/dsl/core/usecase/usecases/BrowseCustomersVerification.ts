import { expect } from '@playwright/test';
import type { BrowseCustomersResponse } from '../../../../driver/port/dtos/BrowseCustomersResponse.js';
import { ResponseVerification } from '../../shared/response-verification.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';

export class BrowseCustomersVerification extends ResponseVerification<BrowseCustomersResponse> {
  constructor(response: BrowseCustomersResponse, context: UseCaseContext) {
    super(response, context);
  }

  /**
   * Each of these customers (usernames) is listed exactly once, in this order. The list is shared, so other
   * customers may come before, between or after them.
   */
  listsCustomersInOrder(expectedCustomers: string[]): this {
    expect(this.getResponse(), 'Response should not be null').not.toBeNull();
    const listed = this.getResponse();
    expect(Array.isArray(listed), `Customers listed should be an array, was ${JSON.stringify(listed)}`).toBe(true);
    for (const customer of expectedCustomers) {
      expect(
        listed.filter((name) => name === customer).length,
        `Times customer '${customer}' is listed, in ${JSON.stringify(listed)}`,
      ).toBe(1);
    }
    const positions = expectedCustomers.map((customer) => listed.indexOf(customer));
    const inOrder = [...positions].sort((a, b) => a - b);
    expect(positions, `Customers listed, in order: ${JSON.stringify(listed)}`).toEqual(inOrder);
    return this;
  }
}
