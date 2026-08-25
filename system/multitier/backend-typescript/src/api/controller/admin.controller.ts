import { Controller, HttpCode, Param, Post } from '@nestjs/common';
import { OrderService } from '../../core/services/order.service';

@Controller('api/admin')
export class AdminController {
  constructor(private readonly orderService: OrderService) {}

  @Post('recall/:sku')
  @HttpCode(200)
  async recallSku(@Param('sku') sku: string) {
    return this.orderService.recallSku(sku);
  }
}
