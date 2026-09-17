import { IsBoolean, IsNumber } from 'class-validator';

export class ErpGetPromotionResponse {
  @IsBoolean()
  promotionActive!: boolean;

  @IsNumber()
  discount!: number;
}
