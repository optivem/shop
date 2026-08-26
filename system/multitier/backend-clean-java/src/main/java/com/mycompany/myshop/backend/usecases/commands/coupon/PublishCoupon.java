package com.mycompany.myshop.backend.usecases.commands.coupon;

import com.mycompany.myshop.backend.domain.entities.Coupon;
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

        // Coupon rejects a discount rate outside (0, 1] by throwing, and nothing here catches it:
        // RefusalTranslatingUseCase answers it with a 422 the same way it answers every other
        // refusal. The try/catch this use case used to carry was a second copy of that boundary,
        // and the kind of thing a seventh use case forgets to write.
        //
        // An absent usage limit is unlimited, and UsageQuota already says that with null. The
        // MAX_VALUE sentinel this used to write is what the legacy services still do.
        var coupon = new Coupon(couponCode, Rate.of(request.getDiscountRate()),
                new ValidityPeriod(request.getValidFrom(), request.getValidTo()),
                UsageQuota.of(request.getUsageLimit(), 0));

        couponRepository.add(coupon);

        return Result.ok();
    }
}
