import type { Result } from '../../../common/result.js';
import type { SystemError } from '../../../driver/port/shop/dtos/errors/SystemError.js';
import type { ScenarioContext } from './scenario-context.js';
import type { ExecutionResult } from './execution-result.js';

export class ExecutionResultBuilder<TResponse> {
  private _result: Result<TResponse, SystemError> | null = null;
  private _context: ScenarioContext | null = null;
  private _orderNumber: string | null = null;
  private _couponCode: string | null = null;

  withResult(result: Result<TResponse, SystemError>): this {
    this._result = result;
    return this;
  }

  withContext(context: ScenarioContext): this {
    this._context = context;
    return this;
  }

  orderNumber(orderNumber: string): this {
    this._orderNumber = orderNumber;
    return this;
  }

  couponCode(code: string): this {
    this._couponCode = code;
    return this;
  }

  getOrderNumber(): string | null {
    return this._orderNumber;
  }

  getCouponCode(): string | null {
    return this._couponCode;
  }

  build(): ExecutionResult<TResponse> {
    if (!this._result) {
      throw new Error('ExecutionResultBuilder: result is required');
    }
    if (!this._context) {
      throw new Error('ExecutionResultBuilder: context is required');
    }
    return { result: this._result, context: this._context };
  }
}
