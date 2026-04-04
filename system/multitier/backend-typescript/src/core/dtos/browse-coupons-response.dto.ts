export class BrowseCouponsItemResponse {
  code: string;
  discountRate: number;
  validFrom: string | null;
  validTo: string | null;
  usageLimit: number | null;
  usedCount: number;
}

export class BrowseCouponsResponse {
  coupons: BrowseCouponsItemResponse[];
}
