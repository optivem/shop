import {
  Controller,
  Get,
  NotFoundException,
  Param,
  ServiceUnavailableException,
} from '@nestjs/common';
import {
  STATUS_SYSTEMS,
  StatusService,
  type StatusSystem,
} from '../../core/services/status.service';

/**
 * Public, component-detail-free status of one part of the system, polled by the storefront's
 * wake-up dialog: 200 {"status":"UP"} or 503 {"status":"DOWN"} (see route-policy.ts).
 */
@Controller('api/status')
export class StatusController {
  constructor(private readonly statusService: StatusService) {}

  @Get(':system')
  async status(@Param('system') system: string): Promise<{ status: 'UP' }> {
    if (!(STATUS_SYSTEMS as readonly string[]).includes(system)) {
      throw new NotFoundException();
    }
    if (!(await this.statusService.isUp(system as StatusSystem))) {
      throw new ServiceUnavailableException({ status: 'DOWN' });
    }
    return { status: 'UP' };
  }
}
