import Decimal from 'decimal.js';

export class BrowseCouponsItemResponse {
  code!: string;
  discountRate!: Decimal;
  validFrom!: string | null;
  validTo!: string | null;
  usageLimit!: number | null;
  usedCount!: number;
}

export class BrowseCouponsResponse {
  coupons!: BrowseCouponsItemResponse[];
}
