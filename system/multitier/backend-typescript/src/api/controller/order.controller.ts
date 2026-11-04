import {
  Body,
  Controller,
  Get,
  Headers,
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
import { Caller, type CurrentUser } from '../../auth/current-user';

@Controller('api/orders')
export class OrderController {
  constructor(private readonly orderService: OrderService) {}

  @Get()
  async browseOrderHistory(
    @Caller() user: CurrentUser,
    @Query('orderNumber') orderNumber?: string,
    @Query('page') page?: string,
    @Query('size') size?: string,
  ) {
    return this.orderService.browseOrderHistory(orderNumber, user, page, size);
  }

  @Post()
  async placeOrder(
    @Body() request: PlaceOrderRequest,
    @Caller() user: CurrentUser,
    @Res({ passthrough: true }) res: Response,
    @Headers('idempotency-key') idempotencyKey?: string,
  ): Promise<PlaceOrderResponse> {
    // A replay answers 201 with the stored body, so the Location is the original order's.
    const response = await this.orderService.placeOrder(
      request,
      user,
      idempotencyKey,
    );
    res.header('Location', `/api/orders/${response.orderNumber}`);
    return response;
  }

  @Get(':orderNumber')
  async getOrder(
    @Param('orderNumber') orderNumber: string,
    @Caller() user: CurrentUser,
  ) {
    return this.orderService.getOrder(orderNumber, user);
  }

  @Post(':orderNumber/cancel')
  @HttpCode(204)
  async cancelOrder(
    @Param('orderNumber') orderNumber: string,
    @Caller() user: CurrentUser,
  ): Promise<void> {
    await this.orderService.cancelOrder(orderNumber, user);
  }

  @Post(':orderNumber/deliver')
  @HttpCode(204)
  async deliverOrder(@Param('orderNumber') orderNumber: string): Promise<void> {
    await this.orderService.deliverOrder(orderNumber);
  }
}
