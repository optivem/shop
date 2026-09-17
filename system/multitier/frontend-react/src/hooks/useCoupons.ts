import { useState, useEffect, useCallback } from 'react';
import { browseCoupons, createCoupon } from '../services/coupon-service';
import type { BrowseCouponsItemResponse } from '../types/api.types';
import type { CouponFormData } from '../features/coupons';

export function useCoupons() {
  const [coupons, setCoupons] = useState<BrowseCouponsItemResponse[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [isCreating, setIsCreating] = useState(false);
  const [reloadCount, setReloadCount] = useState(0);
  // The reload whose response is currently applied; loading is derived from it.
  const [loadedReloadCount, setLoadedReloadCount] = useState<number | null>(null);

  useEffect(() => {
    // Ignore a response superseded by a newer refresh or arriving after unmount.
    let ignore = false;
    void browseCoupons().then((result) => {
      if (ignore) return;
      if (result.success) {
        setCoupons(result.data.coupons);
        setError(null);
      } else {
        setError('Failed to load coupons');
      }
      setLoadedReloadCount(reloadCount);
    });
    return () => {
      ignore = true;
    };
  }, [reloadCount]);

  const refresh = useCallback(() => {
    setReloadCount((count) => count + 1);
  }, []);

  const isLoading = loadedReloadCount !== reloadCount;

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
      Number.parseFloat(formData.discountRate),
      validFrom,
      validTo,
      formData.usageLimit ? Number.parseInt(formData.usageLimit) : null
    );

    setIsCreating(false);

    if (result.success) {
      refresh();
    }

    return result;
  };

  const getCouponStatus = (coupon: BrowseCouponsItemResponse): string => {
    const now = new Date().toISOString();
    const { validFrom, validTo } = coupon;

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
    refresh
  };
}
