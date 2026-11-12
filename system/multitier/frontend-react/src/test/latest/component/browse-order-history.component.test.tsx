// Maintainable contract spec (component level) for BROWSE ORDER HISTORY.
import { describe, it } from 'vitest';
import { componentHarness } from '../../support';

const { backend, frontend } = componentHarness();

describe('BrowseOrderHistory', () => {
  it('shows order history when orders are returned', async () => {
    backend.returnsOrderHistory();
    // The admin page also reads the unfiltered list from page 1 to offer the customers who have ordered.
    backend.returnsOrderHistoryPage(1);

    // Assert the row renders its total and status too, not just the order number — the history table
    // shows Total Price and Status columns. Semantic expectation (total as the wire's decimal string): the
    // UI driver formats it to '$22.00', the gateway driver compares it as-is.
    await frontend
      .browseOrderHistory()
      .execute()
      .showsOrder('ORD-1', { totalPrice: '22.00', status: 'PLACED' });
  });
});
