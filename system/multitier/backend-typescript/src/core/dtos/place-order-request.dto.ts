import { Type } from 'class-transformer';
import {
  IsDefined,
  IsInt,
  IsObject,
  IsOptional,
  IsPositive,
  IsString,
  ValidateIf,
  ValidateNested,
} from 'class-validator';
import {
  BlankAsMissing,
  NumericStringAsNumber,
  TYPE_MISMATCH,
} from './request-parsing';
import { ShippingAddressRequest } from './shipping-address.dto';

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

  // With a shipping address the tax country is the address's, so the top-level country is ignored.
  @BlankAsMissing()
  @ValidateIf((request: PlaceOrderRequest) => request.shippingAddress == null)
  @IsDefined({ message: 'Country must not be empty' })
  @IsString({ message: 'Country must not be empty', context: TYPE_MISMATCH })
  country!: string;

  @BlankAsMissing()
  @IsOptional()
  @IsString({ message: 'Coupon code must be a string', context: TYPE_MISMATCH })
  couponCode?: string;

  @IsOptional()
  @IsObject({
    message: 'Shipping address must be an object',
    context: TYPE_MISMATCH,
  })
  @ValidateNested()
  @Type(() => ShippingAddressRequest)
  shippingAddress?: ShippingAddressRequest | null;
}
