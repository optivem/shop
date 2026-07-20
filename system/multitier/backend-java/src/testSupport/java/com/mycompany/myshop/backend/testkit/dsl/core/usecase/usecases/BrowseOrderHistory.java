package com.mycompany.myshop.backend.support.core.usecase.usecases;

import com.mycompany.myshop.backend.core.dtos.BrowseOrderHistoryResponse;
import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.support.core.usecase.usecases.base.BaseMyShopUseCase;
import org.springframework.http.HttpStatus;

/** {@code GET /api/orders} — always {@code 200 OK}; there is no rejection path to state. */
public class BrowseOrderHistory
        extends BaseMyShopUseCase<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> {

    public BrowseOrderHistory(BackendDriver driver) {
        super(driver);
    }

    @Override
    public UseCaseResult<BrowseOrderHistoryResponse, BrowseOrderHistoryVerification> execute() {
        var response = driver.browseOrderHistory();

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.OK,
            null,
            response::getBody,
            null,
            BrowseOrderHistoryVerification::new);
    }
}
