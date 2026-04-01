import { IsInt, IsNotEmpty, IsPositive, IsString } from 'class-validator';

export class PlaceOrderRequest {
  @IsNotEmpty({ message: 'SKU must not be empty' })
  @IsString({ message: 'SKU must not be empty' })
  sku: string;

  @IsNotEmpty({ message: 'Quantity must not be empty' })
  @IsInt({ message: 'Quantity must be an integer' })
  @IsPositive({ message: 'Quantity must be positive' })
  quantity: number;
}
