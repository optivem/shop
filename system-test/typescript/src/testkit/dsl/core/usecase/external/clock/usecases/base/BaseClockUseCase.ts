import type { ClockDriver } from '../../../../../../../driver/port/external/clock/clock-driver.js';
import { BaseUseCase } from '../../../../../shared/base-use-case.js';

export abstract class BaseClockUseCase<TResponse, TVerification> extends BaseUseCase<
  ClockDriver,
  TResponse,
  TVerification
> {}
