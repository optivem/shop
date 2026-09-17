import Decimal from 'decimal.js';
import {
  IsDate,
  IsDefined,
  IsInt,
  IsOptional,
  IsPositive,
  IsString,
} from 'class-validator';
import {
  BlankAsMissing,
  IsDecimal,
  IsDecimalAtMost,
  IsDecimalGreaterThan,
  NumericStringAsNumber,
  ToDateTime,
  ToDecimal,
  TYPE_MISMATCH,
} from './request-parsing';

export class PublishCouponRequest {
  @BlankAsMissing()
  @IsDefined({ message: 'Coupon code must not be blank' })
  @IsString({
    message: 'Coupon code must not be blank',
    context: TYPE_MISMATCH,
  })
  code!: string;

  @ToDecimal()
  @IsDefined({ message: 'Discount rate must not be null' })
  @IsDecimal({
    message: 'Discount rate must be a number',
    context: TYPE_MISMATCH,
  })
  @IsDecimalGreaterThan(0, {
    message: 'Discount rate must be greater than 0.00',
  })
  @IsDecimalAtMost(1, { message: 'Discount rate must be at most 1.00' })
  discountRate!: Decimal;

  @ToDateTime()
  @IsOptional()
  @IsDate({
    message: 'Valid from must be a valid date-time',
    context: TYPE_MISMATCH,
  })
  validFrom?: Date;

  @ToDateTime()
  @IsOptional()
  @IsDate({
    message: 'Valid to must be a valid date-time',
    context: TYPE_MISMATCH,
  })
  validTo?: Date;

  @NumericStringAsNumber()
  @IsOptional()
  @IsInt({ message: 'Usage limit must be an integer', context: TYPE_MISMATCH })
  @IsPositive({ message: 'Usage limit must be positive' })
  usageLimit?: number;
}
