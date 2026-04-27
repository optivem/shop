import { DEFAULTS } from '../defaults.js';
import { UseCaseContext } from '../../shared/use-case-context.js';
import { AppContext } from '../app-context.js';
import { ScenarioContext } from '../scenario-context.js';
import { ThenCancelOrderResultStage } from '../then/then-cancel-order.js';

export class WhenCancelOrder {
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

  then(): ThenCancelOrderResultStage {
    return new ThenCancelOrderResultStage(this.app, this.ctx, this.useCaseContext, this.orderNumber);
  }
}
