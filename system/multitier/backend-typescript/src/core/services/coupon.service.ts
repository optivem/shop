import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Coupon } from '../entities/coupon.entity';
import { ValidationException } from '../exceptions/validation.exception';
import { ClockGateway } from './external/clock.gateway';

@Injectable()
export class CouponService {
  private static readonly FIELD_COUPON_CODE = 'couponCode';
  private static readonly MSG_COUPON_DOES_NOT_EXIST =
    'Coupon code %s does not exist';
  private static readonly MSG_COUPON_NOT_YET_VALID =
    'Coupon code %s is not yet valid';
  private static readonly MSG_COUPON_EXPIRED = 'Coupon code %s has expired';
  private static readonly MSG_COUPON_USAGE_LIMIT_REACHED =
    'Coupon code %s has exceeded its usage limit';
  private static readonly MSG_COUPON_CODE_ALREADY_EXISTS =
    'Coupon code %s already exists';

  constructor(
    @InjectRepository(Coupon)
    private readonly couponRepository: Repository<Coupon>,
    private readonly clockGateway: ClockGateway,
  ) {}

  async getDiscount(couponCode?: string): Promise<number> {
    if (!couponCode || couponCode.trim() === '') {
      return 0;
    }

    const coupon = await this.couponRepository.findOne({
      where: { code: couponCode },
    });

    if (!coupon) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_DOES_NOT_EXIST,
        couponCode,
      );
    }

    const now = await this.clockGateway.getCurrentTime();

    if (coupon.validFrom && now < coupon.validFrom) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_NOT_YET_VALID,
        couponCode,
      );
    }

    if (coupon.validTo && now > coupon.validTo) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_EXPIRED,
        couponCode,
      );
    }

    if (coupon.usageLimit !== null && coupon.usedCount >= coupon.usageLimit) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_USAGE_LIMIT_REACHED,
        couponCode,
      );
    }

    return Number(coupon.discountRate);
  }

  async incrementUsageCount(couponCode: string): Promise<void> {
    const coupon = await this.couponRepository.findOne({
      where: { code: couponCode },
    });
    if (coupon) {
      coupon.usedCount++;
      await this.couponRepository.save(coupon);
    }
  }

  async createCoupon(
    code: string,
    discountRate: number,
    validFrom?: string,
    validTo?: string,
    usageLimit?: number,
  ): Promise<void> {
    const existing = await this.couponRepository.findOne({ where: { code } });
    if (existing) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_CODE_ALREADY_EXISTS,
        code,
      );
    }

    const coupon = new Coupon();
    coupon.code = code;
    coupon.discountRate = discountRate;
    coupon.validFrom = validFrom ? new Date(validFrom) : null;
    coupon.validTo = validTo ? new Date(validTo) : null;
    coupon.usageLimit = usageLimit ?? null;
    coupon.usedCount = 0;

    await this.couponRepository.save(coupon);
  }

  async getAllCoupons(): Promise<Coupon[]> {
    return this.couponRepository.find();
  }

  private throwCouponValidationException(
    messageTemplate: string,
    couponCode: string,
  ): never {
    throw new ValidationException(
      CouponService.FIELD_COUPON_CODE,
      messageTemplate.replace('%s', couponCode),
    );
  }
}
