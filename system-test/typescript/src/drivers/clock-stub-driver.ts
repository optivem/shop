import { Result, success, failure } from '../common/result.js';
import { ErrorResponse, GetTimeResponse, ReturnsTimeRequest } from '../common/dtos.js';
import { ClockDriver } from './types.js';
import { JsonWireMockClient } from '../clients/wiremock-client.js';

export class ClockStubDriver implements ClockDriver {
  private wireMock: JsonWireMockClient;

  constructor(private baseUrl: string) {
    this.wireMock = new JsonWireMockClient(baseUrl);
  }

  async goToClock(): Promise<Result<void, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/health`);
    if (response.ok) return success(undefined);
    return failure({ message: `Clock stub not available: ${response.status}`, fieldErrors: [] });
  }

  async getTime(): Promise<Result<GetTimeResponse, ErrorResponse>> {
    const response = await fetch(`${this.baseUrl}/api/time`);
    if (response.ok) {
      const data = (await response.json()) as GetTimeResponse;
      return success(data);
    }
    return failure({ message: `Failed to get time: ${response.status}`, fieldErrors: [] });
  }

  async returnsTime(request: ReturnsTimeRequest): Promise<Result<void, ErrorResponse>> {
    await this.wireMock.stubGet('/clock/api/time', { time: request.time });
    return success(undefined);
  }

  async close(): Promise<void> {
    await this.wireMock.removeStubs();
  }
}
