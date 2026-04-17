import {
  IsInt,
  IsNotEmpty,
  IsOptional,
  IsPositive,
  IsString,
  Matches,
} from 'class-validator';

export class PlaceOrderRequest {
  @IsNotEmpty({ message: 'SKU must not be empty' })
  @Matches(/\S/, { message: 'SKU must not be empty' })
  @IsString({ message: 'SKU must not be empty' })
  sku: string;

  @IsNotEmpty({ message: 'Quantity must not be empty' })
  @IsInt({ message: 'Quantity must be an integer' })
  @IsPositive({ message: 'Quantity must be positive' })
  quantity: number;

  @IsNotEmpty({ message: 'Country must not be empty' })
  @Matches(/\S/, { message: 'Country must not be empty' })
  @IsString({ message: 'Country must not be empty' })
  country: string;

  @IsOptional()
  couponCode?: string;
}
