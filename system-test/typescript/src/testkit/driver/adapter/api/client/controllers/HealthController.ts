import type { Result } from '../../../../../common/result.js';
import { success, failure } from '../../../../../common/result.js';
import type { SystemError } from '../../../../port/dtos/errors/SystemError.js';

export class HealthController {
  constructor(private readonly baseUrl: string) {}

  async checkHealth(): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({
      message: `MyShop API not available: ${response.status}`,
      fieldErrors: [],
    });
  }

  /** Readiness: answers 200 only when the system can serve requests, its database included. */
  async checkReadiness(): Promise<Result<void, SystemError>> {
    const response = await fetch(`${this.baseUrl}/readyz`);
    if (response.ok) return success(undefined);
    return failure({
      message: `MyShop API not ready (${this.baseUrl}/readyz): ${response.status} ${(await response.text()).slice(0, 300)}`,
      fieldErrors: [],
    });
  }
}
