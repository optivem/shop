import { Body, Controller, Get, Param, Post, Query, Res } from '@nestjs/common';
import type { Response } from 'express';
import { OrderService } from '../../core/services/order.service';
import { PlaceOrderRequest } from '../../core/dtos/place-order-request.dto';

@Controller('api/orders')
export class OrderController {
  constructor(private readonly orderService: OrderService) {}

  @Get()
  async browseOrderHistory(@Query('orderNumber') orderNumber?: string) {
    return this.orderService.browseOrderHistory(orderNumber);
  }

  @Post()
  async placeOrder(@Body() request: PlaceOrderRequest, @Res() res: Response) {
    const response = await this.orderService.placeOrder(request);
    res.status(201)
      .header('Location', `/api/orders/${response.orderNumber}`)
      .json(response);
  }

  @Get(':orderNumber')
  async getOrder(@Param('orderNumber') orderNumber: string) {
    return this.orderService.getOrder(orderNumber);
  }
}
