import {
  IsDefined,
  IsInt,
  IsOptional,
  IsPositive,
  IsString,
} from 'class-validator';
import {
  BlankAsMissing,
  NumericStringAsNumber,
  TYPE_MISMATCH,
} from './request-parsing';

export class PlaceOrderRequest {
  @BlankAsMissing()
  @IsDefined({ message: 'SKU must not be empty' })
  @IsString({ message: 'SKU must not be empty', context: TYPE_MISMATCH })
  sku!: string;

  @NumericStringAsNumber()
  @IsDefined({ message: 'Quantity must not be empty' })
  @IsInt({ message: 'Quantity must be an integer', context: TYPE_MISMATCH })
  @IsPositive({ message: 'Quantity must be positive' })
  quantity!: number;

  @BlankAsMissing()
  @IsDefined({ message: 'Country must not be empty' })
  @IsString({ message: 'Country must not be empty', context: TYPE_MISMATCH })
  country!: string;

  @BlankAsMissing()
  @IsOptional()
  @IsString({ message: 'Coupon code must be a string', context: TYPE_MISMATCH })
  couponCode?: string;
}
