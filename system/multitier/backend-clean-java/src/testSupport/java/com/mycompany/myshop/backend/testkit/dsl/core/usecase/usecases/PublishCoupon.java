package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.backend.usecases.coupon.PublishCouponRequest;
import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import org.springframework.http.HttpStatus;

public class PublishCoupon extends BaseMyShopUseCase<Void, VoidVerification> {

    private String couponCode;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private Integer usageLimit;

    public PublishCoupon(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    public PublishCoupon couponCode(String couponCode) {
        this.couponCode = couponCode;
        return this;
    }

    public PublishCoupon discountRate(String discountRate) {
        this.discountRate = discountRate;
        return this;
    }

    public PublishCoupon validFrom(String validFrom) {
        this.validFrom = validFrom;
        return this;
    }

    public PublishCoupon validTo(String validTo) {
        this.validTo = validTo;
        return this;
    }

    public PublishCoupon usageLimit(Integer usageLimit) {
        this.usageLimit = usageLimit;
        return this;
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var request = new PublishCouponRequest();
        request.setCode(couponCode);
        request.setDiscountRate(discountRate == null ? null : new BigDecimal(discountRate));
        request.setValidFrom(validFrom == null ? null : Instant.parse(validFrom));
        request.setValidTo(validTo == null ? null : Instant.parse(validTo));
        request.setUsageLimit(usageLimit);

        var response = driver.publishCoupon(request);

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.NO_CONTENT,
            Set.of(),
            () -> null,
            null,
            VoidVerification::new);
    }
}
