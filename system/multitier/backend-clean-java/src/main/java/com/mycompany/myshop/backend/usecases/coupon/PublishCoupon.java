package com.mycompany.myshop.backend.usecases.coupon;

import com.mycompany.myshop.backend.domain.entities.Coupon;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.repositories.CouponRepository;
import com.mycompany.myshop.backend.domain.values.CouponCode;
import com.mycompany.myshop.backend.domain.values.Rate;
import com.mycompany.myshop.backend.domain.values.UsageQuota;
import com.mycompany.myshop.backend.domain.values.ValidityPeriod;
import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.usecases.UseCase;
import com.mycompany.myshop.backend.usecases.UseCaseError;

public class PublishCoupon implements UseCase<PublishCouponRequest, Void> {

    private static final String MSG_COUPON_CODE_ALREADY_EXISTS = "Coupon code %s already exists";

    private final CouponRepository couponRepository;

    public PublishCoupon(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public Result<Void, UseCaseError> execute(PublishCouponRequest request) {
        var couponCode = CouponCode.of(request.getCode());

        if (couponRepository.findByCode(couponCode).isPresent()) {
            return Result.err(new UseCaseError.Invalid(CouponCode.FIELD_NAME,
                    String.format(MSG_COUPON_CODE_ALREADY_EXISTS, couponCode)));
        }

        // The catch is the same boundary every other use case has: the domain states its rules by
        // throwing, and this is where a throw becomes a returned error. Without it, Coupon rejecting
        // a discount rate outside (0, 1] would leave here as an unhandled exception and be answered
        // with a 500 -- masked today only because PublishCouponRequest repeats the same bound as a
        // bean-validation annotation one layer out.
        try {
            // An absent usage limit is unlimited, and UsageQuota already says that with null. The
            // MAX_VALUE sentinel this used to write is what the legacy services still do.
            var coupon = new Coupon(couponCode, Rate.of(request.getDiscountRate()),
                    new ValidityPeriod(request.getValidFrom(), request.getValidTo()),
                    UsageQuota.of(request.getUsageLimit(), 0));

            couponRepository.add(coupon);
        } catch (ValidationException e) {
            return Result.err(UseCaseError.from(e));
        }

        return Result.ok(null);
    }
}
