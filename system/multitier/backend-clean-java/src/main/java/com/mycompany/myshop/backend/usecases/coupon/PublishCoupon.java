package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import com.mycompany.myshop.backend.usecases.dtos.PublishCouponRequest;

/**
 * Publishes a new coupon.
 */
public class PublishCoupon {

    private static final String MSG_COUPON_CODE_ALREADY_EXISTS = "Coupon code %s already exists";

    private final CouponRepository couponRepository;

    public PublishCoupon(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    public void execute(PublishCouponRequest request) {
        var couponCode = CouponCode.of(request.getCode());

        if (couponRepository.findByCode(couponCode).isPresent()) {
            throw new ValidationException(CouponCode.FIELD_NAME,
                    String.format(MSG_COUPON_CODE_ALREADY_EXISTS, couponCode));
        }

        // If usageLimit is null, set to unlimited (Integer.MAX_VALUE). UsageQuota would take the null
        // directly, but the published row records MAX_VALUE and callers read it back — left as is.
        var usageLimit = request.getUsageLimit();
        int limit = usageLimit != null ? usageLimit : Integer.MAX_VALUE;

        var coupon = new Coupon(couponCode, Rate.of(request.getDiscountRate()),
                new ValidityPeriod(request.getValidFrom(), request.getValidTo()),
                UsageQuota.of(limit, 0));

        couponRepository.save(coupon);
    }
}
