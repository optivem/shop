import { DEFAULTS } from '../defaults.js';
import type { UseCaseContext } from '../../shared/use-case-context.js';
import type { AppContext } from '../app-context.js';
import type { ScenarioContext } from '../scenario-context.js';
import { ThenBrowseOrderHistoryResultStage } from '../then/then-browse-order-history.js';
import { assertNotAwaited } from '../assert-not-awaited.js';

export class WhenBrowseOrderHistory {
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

  then(): ThenBrowseOrderHistoryResultStage;
  then(...args: unknown[]): ThenBrowseOrderHistoryResultStage {
    assertNotAwaited(args);
    this.ctx.markExecuted();
    return new ThenBrowseOrderHistoryResultStage(this.app, this.ctx, this.useCaseContext, this.orderNumber);
  }
}
