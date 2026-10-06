import { expect } from '@playwright/test';
import { UserIdentity } from '../../../../driver/port/user-identity.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import { DEFAULTS } from '../defaults.js';
import type { OrderConfig, ScenarioContext } from '../scenario-context.js';

/**
 * Sets up a given order: places it as its customer (cancelling it as the same customer when it is to end up
 * cancelled), then delivers it as the default identity (admin) when it is to end up delivered. The order's
 * alias resolves to the number the system assigned to it.
 */
export async function placeGivenOrder(
  app: AppContext,
  ctx: ScenarioContext,
  useCaseContext: UseCaseContext,
  order: OrderConfig,
): Promise<void> {
  const alias = order.orderNumber ?? DEFAULTS.ORDER_NUMBER;

  app.actAs(ctx.placingIdentity(order));
  try {
    const placeResult = await app.myShop().placeOrder({
      sku: useCaseContext.getParamValue(order.sku),
      quantity: order.quantity,
      country: useCaseContext.getParamValueOrLiteral(order.country),
      couponCode: useCaseContext.getParamValue(order.couponCode),
    });
    expect(placeResult.success, `Given order '${alias}' could not be placed: ${JSON.stringify(placeResult)}`).toBe(true);
    if (!placeResult.success) return;

    const orderNumber = placeResult.value.orderNumber;
    useCaseContext.setResultEntry(alias, orderNumber);
    order.orderNumber = orderNumber;

    if (order.status === 'CANCELLED') {
      const cancelResult = await app.myShop().cancelOrder({ orderNumber });
      expect(cancelResult.success, `Given order '${alias}' could not be cancelled: ${JSON.stringify(cancelResult)}`).toBe(true);
    }
  } finally {
    app.actAs(UserIdentity.DEFAULT);
  }

  if (order.status === 'DELIVERED' && order.orderNumber) {
    const deliverResult = await app.myShop().deliverOrder({ orderNumber: order.orderNumber });
    expect(deliverResult.success, `Given order '${alias}' could not be delivered: ${JSON.stringify(deliverResult)}`).toBe(true);
  }
}
