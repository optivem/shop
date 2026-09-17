import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ClockGetTimeResponse } from '../../dtos/external/clock-get-time-response.dto';
import { errorMessage, fetchJson } from './fetch-json';

type ExternalSystemMode = 'real' | 'stub';

const isExternalSystemMode = (value: string): value is ExternalSystemMode =>
  value === 'real' || value === 'stub';

@Injectable()
export class ClockGateway {
  private readonly externalSystemMode: ExternalSystemMode;
  private readonly clockUrl: string;

  constructor(private readonly configService: ConfigService) {
    const externalSystemMode = this.configService.get<string>(
      'EXTERNAL_SYSTEM_MODE',
      'real',
    );
    if (!isExternalSystemMode(externalSystemMode)) {
      throw new Error(`Unknown external system mode: ${externalSystemMode}`);
    }
    this.externalSystemMode = externalSystemMode;
    this.clockUrl = this.configService.get<string>(
      'CLOCK_API_URL',
      'http://localhost:9001/clock',
    );
  }

  async getCurrentTime(): Promise<Date> {
    if (this.externalSystemMode === 'real') {
      return new Date();
    }
    return this.getStubTime();
  }

  private async getStubTime(): Promise<Date> {
    const url = `${this.clockUrl}/api/time`;
    try {
      const clockResponse = await fetchJson(url, ClockGetTimeResponse);
      return new Date(clockResponse.time);
    } catch (e) {
      throw new Error(`Failed to fetch current time. ${errorMessage(e)}`, {
        cause: e,
      });
    }
  }
}
