export interface PublishCouponRequest {
  code: string;
  discountRate: number;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number | string;
}
