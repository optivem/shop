package com.mycompany.myshop.backend.support.core.usecase.usecases;

import com.mycompany.myshop.backend.core.dtos.PublishCouponRequest;
import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.support.core.shared.VoidVerification;
import com.mycompany.myshop.backend.support.core.usecase.usecases.base.BaseMyShopUseCase;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.http.HttpStatus;

/**
 * {@code POST /api/coupons} — {@code 204 No Content}, no body to parse.
 *
 * <p>No rejection contract is declared: {@code BackendDriver} binds the response to {@code Void}, so
 * a {@code ProblemDetail} body would be discarded before the DSL could assert it. {@code
 * shouldFail()} on a publish therefore fails loudly rather than asserting nothing. No component test
 * exercises a rejected publish today; the day one does, the driver returns the body as {@code
 * String} and the contract is declared here.
 */
public class PublishCoupon extends BaseMyShopUseCase<Void, VoidVerification> {

    private String couponCode;
    private String discountRate;
    private String validFrom;
    private String validTo;
    private Integer usageLimit;

    public PublishCoupon(BackendDriver driver) {
        super(driver);
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
            null,
            () -> null,
            null,
            VoidVerification::new);
    }
}
