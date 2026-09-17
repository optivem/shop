import Decimal from 'decimal.js';
import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { EntityManager, Repository } from 'typeorm';
import { Coupon } from '../entities/coupon.entity';
import { PublishCouponRequest } from '../dtos/publish-coupon-request.dto';
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

  async getDiscount(couponCode?: string): Promise<Decimal> {
    if (!couponCode || couponCode.trim() === '') {
      return new Decimal(0);
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

    return coupon.discountRate;
  }

  // The limit check and the increment are a single conditional UPDATE, so concurrent orders
  // cannot exceed the usage limit.
  async claimUsage(couponCode: string, manager: EntityManager): Promise<void> {
    const result = await manager
      .createQueryBuilder()
      .update(Coupon)
      .set({ usedCount: () => 'used_count + 1' })
      .where('code = :couponCode', { couponCode })
      .andWhere('(usage_limit IS NULL OR used_count < usage_limit)')
      .execute();

    if (result.affected !== 1) {
      this.throwCouponValidationException(
        CouponService.MSG_COUPON_USAGE_LIMIT_REACHED,
        couponCode,
      );
    }
  }

  async createCoupon({
    code,
    discountRate,
    validFrom,
    validTo,
    usageLimit,
  }: PublishCouponRequest): Promise<void> {
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
    coupon.validFrom = validFrom ?? null;
    coupon.validTo = validTo ?? null;
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
