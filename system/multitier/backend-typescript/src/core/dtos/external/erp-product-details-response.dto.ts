import { IsNumber, IsOptional, IsString } from 'class-validator';

// ERP product JSON: {id, title, description, price, category, brand, imageUrl}. Order placement needs only
// the price; the catalog endpoints pass the descriptive fields through.
export class ErpProductDetailsResponse {
  @IsOptional()
  @IsString()
  id?: string;

  @IsOptional()
  @IsString()
  title?: string;

  @IsOptional()
  @IsString()
  description?: string;

  @IsNumber()
  price!: number;

  @IsOptional()
  @IsString()
  category?: string;

  @IsOptional()
  @IsString()
  brand?: string;

  // Where the ERP serves the product photo, as a path on the ERP (e.g. /erp/images/HP-15.jpg).
  @IsOptional()
  @IsString()
  imageUrl?: string;
}
