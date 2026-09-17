import {
  Body,
  Controller,
  Get,
  HttpCode,
  Param,
  Post,
  Query,
  Res,
} from '@nestjs/common';
import type { Response } from 'express';
import { OrderService } from '../../core/services/order.service';
import { PlaceOrderRequest } from '../../core/dtos/place-order-request.dto';
import { PlaceOrderResponse } from '../../core/dtos/place-order-response.dto';

@Controller('api/orders')
export class OrderController {
  constructor(private readonly orderService: OrderService) {}

  @Get()
  async browseOrderHistory(@Query('orderNumber') orderNumber?: string) {
    return this.orderService.browseOrderHistory(orderNumber);
  }

  @Post()
  async placeOrder(
    @Body() request: PlaceOrderRequest,
    @Res({ passthrough: true }) res: Response,
  ): Promise<PlaceOrderResponse> {
    const response = await this.orderService.placeOrder(request);
    res.header('Location', `/api/orders/${response.orderNumber}`);
    return response;
  }

  @Get(':orderNumber')
  async getOrder(@Param('orderNumber') orderNumber: string) {
    return this.orderService.getOrder(orderNumber);
  }

  @Post(':orderNumber/cancel')
  @HttpCode(204)
  async cancelOrder(@Param('orderNumber') orderNumber: string): Promise<void> {
    await this.orderService.cancelOrder(orderNumber);
  }

  @Post(':orderNumber/deliver')
  @HttpCode(204)
  async deliverOrder(@Param('orderNumber') orderNumber: string): Promise<void> {
    await this.orderService.deliverOrder(orderNumber);
  }
}
