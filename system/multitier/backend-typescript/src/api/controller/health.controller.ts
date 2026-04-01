import { Controller, Get } from '@nestjs/common';

@Controller()
export class HealthController {
  @Get('health')
  checkHealth() {
    return { status: 'UP' };
  }
}
