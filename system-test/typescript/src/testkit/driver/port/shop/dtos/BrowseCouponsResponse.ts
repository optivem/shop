export interface BrowseCouponItem {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount: number;
}

export interface BrowseCouponsResponse {
  coupons: BrowseCouponItem[];
}
