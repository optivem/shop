import {
  IsNotEmpty,
  IsNumber,
  IsOptional,
  IsPositive,
  Matches,
  Max,
  Min,
} from 'class-validator';

export class PublishCouponRequest {
  @IsNotEmpty({ message: 'Coupon code must not be blank' })
  @Matches(/\S/, { message: 'Coupon code must not be blank' })
  code: string;

  @IsNumber({}, { message: 'Discount rate must not be null' })
  @Min(0.0001, { message: 'Discount rate must be greater than 0.00' })
  @Max(1, { message: 'Discount rate must be at most 1.00' })
  discountRate: number;

  @IsOptional()
  validFrom?: string;

  @IsOptional()
  validTo?: string;

  @IsOptional()
  @IsPositive({ message: 'Usage limit must be positive' })
  usageLimit?: number;
}
