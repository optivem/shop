import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenDeliverOrderResultStage } from '../then/then-deliver-order.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenDeliverOrder {
  private orderNumber: string = DEFAULTS.ORDER_NUMBER;

  constructor(
    private readonly app: AppContext,
    private readonly ctx: ScenarioContext,
    private readonly useCaseContext: UseCaseContext,
  ) {}

  withOrderNumber(orderNumber: string): this {
    this.orderNumber = orderNumber;
    return this;
  }

  then(): ThenDeliverOrderResultStage;
  then(...args: unknown[]): ThenDeliverOrderResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenDeliverOrderResultStage(this.app, this.ctx, this.useCaseContext, this.orderNumber);
  }
}
