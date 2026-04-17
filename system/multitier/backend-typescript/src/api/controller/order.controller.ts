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
    res
      .status(201)
      .header('Location', `/api/orders/${response.orderNumber}`)
      .json(response);
  }

  @Get(':orderNumber')
  async getOrder(@Param('orderNumber') orderNumber: string) {
    return this.orderService.getOrder(orderNumber);
  }

  @Post(':orderNumber/cancel')
  async cancelOrder(
    @Param('orderNumber') orderNumber: string,
    @Res() res: Response,
  ) {
    await this.orderService.cancelOrder(orderNumber);
    res.status(204).send();
  }

  @Post(':orderNumber/deliver')
  async deliverOrder(
    @Param('orderNumber') orderNumber: string,
    @Res() res: Response,
  ) {
    await this.orderService.deliverOrder(orderNumber);
    res.status(204).send();
  }
}
