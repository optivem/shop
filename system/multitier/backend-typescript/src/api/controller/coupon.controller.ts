import { Body, Controller, Get, HttpCode, Post } from '@nestjs/common';
import { CouponService } from '../../core/services/coupon.service';
import { PublishCouponRequest } from '../../core/dtos/publish-coupon-request.dto';
import {
  BrowseCouponsItemResponse,
  BrowseCouponsResponse,
} from '../../core/dtos/browse-coupons-response.dto';

@Controller('api/coupons')
export class CouponController {
  constructor(private readonly couponService: CouponService) {}

  @Post()
  @HttpCode(204)
  async createCoupon(@Body() request: PublishCouponRequest): Promise<void> {
    await this.couponService.createCoupon(
      request.code,
      request.discountRate,
      request.validFrom,
      request.validTo,
      request.usageLimit,
    );
  }

  @Get()
  async browseCoupons(): Promise<BrowseCouponsResponse> {
    const coupons = await this.couponService.getAllCoupons();
    const items: BrowseCouponsItemResponse[] = coupons.map((c) => ({
      code: c.code,
      discountRate: Number(c.discountRate),
      validFrom: c.validFrom ? new Date(c.validFrom).toISOString() : null,
      validTo: c.validTo ? new Date(c.validTo).toISOString() : null,
      usageLimit: c.usageLimit,
      usedCount: c.usedCount,
    }));
    return { coupons: items };
  }
}
