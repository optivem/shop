import { Controller, Get, ServiceUnavailableException } from '@nestjs/common';
import { DataSource } from 'typeorm';

/**
 * /health is the system tests' probe. /livez (process up, no dependency checked) and /readyz (database
 * reachable) are the split liveness/readiness probes, public and without component details, like the
 * Java backends' actuator probes; the Docker HEALTHCHECK probes /livez.
 */
@Controller()
export class HealthController {
  constructor(private readonly dataSource: DataSource) {}

  @Get('health')
  checkHealth() {
    return { status: 'UP' };
  }

  @Get('livez')
  liveness() {
    return { status: 'UP' };
  }

  @Get('readyz')
  async readiness() {
    try {
      await this.dataSource.query('SELECT 1');
    } catch {
      throw new ServiceUnavailableException({ status: 'DOWN' });
    }
    return { status: 'UP' };
  }
}
