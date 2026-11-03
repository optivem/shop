// Maintainable contract spec (component level) for CANCEL ORDER — the frontend twin of the system
// test's latest/acceptance/CancelOrder{Positive,Negative}Test.
//
// Cancelling has no screen of its own: the user opens an order and presses the action there, so
// each spec stages the order it opens AND the cancel it presses. What the frontend depends on is
// only the STATUS CODE — the backend answers 204 with no body — plus, when refused, the reason it
// has to put on the screen.
//
// The already-cancelled rejection from the acceptance suite has no twin here: the Cancel button is
// hidden for a CANCELLED order (view-order.component.test.tsx pins that), so a user cannot provoke
// it. The blackout is the one cancel rejection the UI can reach, and therefore the one it renders.
import { describe, it } from 'vitest';
import { componentHarness } from '../../support';

const { backend, frontend } = componentHarness();

describe('CancelOrder', () => {
  it('confirms the cancellation when the backend accepts it', async () => {
    backend.returnsOrderDetails('ORD-1');
    backend.acceptsCancelOrder('ORD-1');

    await frontend.cancelOrder('ORD-1').execute().wasCancelled();
  });

  it('shows the reason when the backend refuses to cancel', async () => {
    backend.returnsOrderDetails('ORD-1');
    backend.rejectsCancelOrderDuringBlackout('ORD-1');

    await frontend
      .cancelOrder('ORD-1')
      .execute()
      .wasRejected('Order cancellation is not allowed on December 31st between 22:00 and 22:30');
  });
});
