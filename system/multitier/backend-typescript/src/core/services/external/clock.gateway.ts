import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { GetTimeResponse } from '../../dtos/external/get-time-response.dto';

@Injectable()
export class ClockGateway {
  private readonly externalSystemMode: string;
  private readonly clockUrl: string;

  constructor(private readonly configService: ConfigService) {
    this.externalSystemMode = this.configService.get<string>(
      'EXTERNAL_SYSTEM_MODE',
      'real',
    );
    this.clockUrl = this.configService.get<string>(
      'CLOCK_API_URL',
      'http://localhost:9001/clock',
    );
  }

  async getCurrentTime(): Promise<Date> {
    if (this.externalSystemMode === 'real') {
      return new Date();
    } else if (this.externalSystemMode === 'stub') {
      return this.getStubTime();
    } else {
      throw new Error(
        `Unknown external system mode: ${this.externalSystemMode}`,
      );
    }
  }

  private async getStubTime(): Promise<Date> {
    const url = `${this.clockUrl}/api/time`;

    try {
      const response = await fetch(url, {
        signal: AbortSignal.timeout(10000),
      });

      if (!response.ok) {
        const body = await response.text();
        throw new Error(
          `Clock API returned status ${response.status}. URL: ${url}. Response: ${body}`,
        );
      }

      const clockResponse = (await response.json()) as GetTimeResponse;
      return new Date(clockResponse.time);
    } catch (e) {
      if (e instanceof Error && e.message.startsWith('Clock API returned')) {
        throw e;
      }
      const err = e as Error;
      throw new Error(
        `Failed to fetch current time from URL: ${this.clockUrl}. Error: ${err.constructor.name}: ${err.message}`,
      );
    }
  }
}
