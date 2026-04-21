import type { Result } from '../../../common/result.js';
import type { SystemError } from '../../../driver/port/shop/dtos/errors/SystemError.js';
import type { ScenarioContext } from './scenario-context.js';

export interface ExecutionResult<TResponse> {
  result: Result<TResponse, SystemError>;
  context: ScenarioContext;
}
