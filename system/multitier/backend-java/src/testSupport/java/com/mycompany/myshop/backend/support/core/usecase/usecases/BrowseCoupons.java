package com.mycompany.myshop.backend.support.core.usecase.usecases;

import com.mycompany.myshop.backend.core.dtos.BrowseCouponsResponse;
import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.support.core.usecase.usecases.base.BaseMyShopUseCase;
import org.springframework.http.HttpStatus;

/** {@code GET /api/coupons} — always {@code 200 OK}; there is no rejection path to state. */
public class BrowseCoupons extends BaseMyShopUseCase<BrowseCouponsResponse, BrowseCouponsVerification> {

    public BrowseCoupons(BackendDriver driver) {
        super(driver);
    }

    @Override
    public UseCaseResult<BrowseCouponsResponse, BrowseCouponsVerification> execute() {
        var response = driver.browseCoupons();

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.OK,
            null,
            response::getBody,
            null,
            BrowseCouponsVerification::new);
    }
}
