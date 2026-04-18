import { success, failure, type Result } from '../../../../../common/result.js';
import type { SystemError } from '../../../../../driver/port/shop/dtos/errors/SystemError.js';

export class SystemResults {
  private constructor() {
    // Utility class
  }

  static success<T>(value: T): Result<T, SystemError>;
  static success(): Result<void, SystemError>;
  static success<T>(value?: T): Result<T | void, SystemError> {
    return success(value as T);
  }

  static failure<T>(error: SystemError | string): Result<T, SystemError> {
    if (typeof error === 'string') {
      return failure({ message: error, fieldErrors: [] });
    }
    return failure(error);
  }
}
