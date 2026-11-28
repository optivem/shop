import { Injectable } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { DataSource } from 'typeorm';

export const STATUS_SYSTEMS = ['backend', 'erp', 'tax', 'clock'] as const;
export type StatusSystem = (typeof STATUS_SYSTEMS)[number];

const PROBE_TIMEOUT_MS = 10_000;

/**
 * Answers "is this part of the system up?" for the storefront's wake-up dialog. The external systems
 * are probed with a plain fetch, deliberately outside the gateways' resilience wrapper: a probe must
 * neither retry nor feed the circuit breaker that real traffic relies on. Calling them also wakes
 * them early (they scale to zero).
 */
@Injectable()
export class StatusService {
  constructor(
    private readonly dataSource: DataSource,
    private readonly configService: ConfigService,
  ) {}

  async isUp(system: StatusSystem): Promise<boolean> {
    switch (system) {
      case 'backend':
        return this.databaseAnswers();
      case 'erp':
        return this.healthAnswers(
          this.configService.get<string>(
            'ERP_API_URL',
            'http://localhost:9001/erp',
          ),
        );
      case 'tax':
        return this.healthAnswers(
          this.configService.get<string>(
            'TAX_API_URL',
            'http://localhost:9001/tax',
          ),
        );
      case 'clock':
        // Real mode reads the system clock, so there is nothing to call.
        if (
          this.configService.get<string>('EXTERNAL_SYSTEM_MODE', 'real') ===
          'real'
        ) {
          return true;
        }
        return this.healthAnswers(
          this.configService.get<string>(
            'CLOCK_API_URL',
            'http://localhost:9001/clock',
          ),
        );
    }
  }

  private async databaseAnswers(): Promise<boolean> {
    try {
      await this.dataSource.query('SELECT 1');
      return true;
    } catch {
      return false;
    }
  }

  private async healthAnswers(baseUrl: string): Promise<boolean> {
    try {
      const response = await fetch(`${baseUrl}/health`, {
        signal: AbortSignal.timeout(PROBE_TIMEOUT_MS),
      });
      await response.body?.cancel();
      return response.ok;
    } catch {
      return false;
    }
  }
}
