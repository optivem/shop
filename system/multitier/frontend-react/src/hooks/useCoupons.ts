import { useState, useEffect, useCallback } from 'react';
import { browseCoupons, createCoupon } from '../services/coupon-service';
import type { BrowseCouponsItemResponse } from '../types/api.types';
import type { CouponFormData } from '../features/coupons';

export function useCoupons() {
  const [coupons, setCoupons] = useState<BrowseCouponsItemResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);

  const loadCoupons = useCallback(async () => {
    setIsLoading(true);
    setError(null);

    const result = await browseCoupons();

    if (result.success) {
      setCoupons(result.data.coupons);
    } else {
      setError('Failed to load coupons');
    }
    setIsLoading(false);
  }, []);

  useEffect(() => {
    loadCoupons();
  }, [loadCoupons]);

  const generateCouponCode = (): string => {
    const randomNum = Math.floor(1000 + Math.random() * 9000);
    return `SAVE${randomNum}`;
  };

  const submitCoupon = async (formData: CouponFormData) => {
    setError(null);
    setIsCreating(true);

    const validFrom = formData.validFrom?.trim()
      ? new Date(formData.validFrom + 'Z').toISOString()
      : null;
    const validTo = formData.validTo?.trim()
      ? new Date(formData.validTo + 'Z').toISOString()
      : null;

    const result = await createCoupon(
      formData.code,
      formData.discountRate,
      validFrom,
      validTo,
      formData.usageLimit ? Number.parseInt(formData.usageLimit) : null
    );

    setIsCreating(false);

    if (result.success) {
      setTimeout(async () => {
        await loadCoupons();
      }, 100);
    }

    return result;
  };

  const getCouponStatus = (coupon: BrowseCouponsItemResponse): string => {
    const now = new Date().toISOString();
    const validFrom = coupon.validFrom ? coupon.validFrom : null;
    const validTo = coupon.validTo ? coupon.validTo : null;

    if (validFrom && now < validFrom) {
      return 'Not Yet Valid';
    } else if (validTo && now > validTo) {
      return 'Expired';
    } else if (coupon.usageLimit !== null && coupon.usedCount >= coupon.usageLimit) {
      return 'Limit Reached';
    }
    return 'Active';
  };

  return {
    coupons,
    isLoading,
    error,
    isCreating,
    submitCoupon,
    generateCouponCode,
    getCouponStatus,
    refresh: loadCoupons
  };
}
