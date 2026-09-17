import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { ClockGetTimeResponse } from '../../dtos/external/clock-get-time-response.dto';
import { errorMessage, fetchJson } from './fetch-json';

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
      const clockResponse = await fetchJson(url, ClockGetTimeResponse);
      return new Date(clockResponse.time);
    } catch (e) {
      throw new Error(`Failed to fetch current time. ${errorMessage(e)}`, {
        cause: e,
      });
    }
  }
}
