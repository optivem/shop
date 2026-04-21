import type { Result } from '../../../../../../common/result.js';
import { success, failure } from '../../../../../../common/result.js';
import type { SystemError } from '../../../../../port/shop/dtos/errors/SystemError.js';

export class HealthController {
  constructor(private readonly baseUrl: string) {}

  async checkHealth(): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({
      message: `Shop API not available: ${response.status}`,
      fieldErrors: [],
    });
  }
}
