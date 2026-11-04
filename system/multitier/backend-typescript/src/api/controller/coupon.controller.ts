import { Body, Controller, Get, HttpCode, Post, Query } from '@nestjs/common';
import { CouponService } from '../../core/services/coupon.service';
import { PublishCouponRequest } from '../../core/dtos/publish-coupon-request.dto';
import { BrowseCouponsResponse } from '../../core/dtos/browse-coupons-response.dto';

@Controller('api/coupons')
export class CouponController {
  constructor(private readonly couponService: CouponService) {}

  @Post()
  @HttpCode(204)
  async createCoupon(@Body() request: PublishCouponRequest): Promise<void> {
    await this.couponService.createCoupon(request);
  }

  @Get()
  async browseCoupons(
    @Query('page') page?: string,
    @Query('size') size?: string,
  ): Promise<BrowseCouponsResponse> {
    return this.couponService.browseCoupons(page, size);
  }
}
