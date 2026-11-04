export interface BrowseCouponItem {
  code: string;
  discountRate: string;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number;
  usedCount: number;
}

export interface BrowseCouponsResponse {
  coupons: BrowseCouponItem[];
}
