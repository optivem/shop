export interface PublishCouponRequest {
  code: string;
  discountRate: number | string;
  validFrom?: string;
  validTo?: string;
  usageLimit?: number | string;
}
