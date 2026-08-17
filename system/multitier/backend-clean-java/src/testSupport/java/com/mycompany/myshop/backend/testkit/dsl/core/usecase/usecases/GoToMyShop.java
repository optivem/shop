package com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases;

import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.testkit.driver.port.MyShopDriver;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseContext;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.UseCaseResult;
import com.mycompany.myshop.backend.testkit.dsl.core.shared.VoidVerification;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.usecases.base.BaseMyShopUseCase;
import java.util.Set;
import org.springframework.http.HttpStatus;

/**
 * The liveness probe behind {@code assume().myShop().shouldBeRunning()}: {@code GET /health} must
 * answer {@code 200 OK} with {@code status: UP}. Both halves of that contract are asserted by
 * {@code shouldSucceed()} — the status by {@link UseCaseResult}, the body here.
 */
public class GoToMyShop extends BaseMyShopUseCase<Void, VoidVerification> {

    public GoToMyShop(MyShopDriver driver, UseCaseContext context) {
        super(driver, context);
    }

    @Override
    public UseCaseResult<Void, VoidVerification> execute() {
        var response = driver.checkHealth();

        return new UseCaseResult<>(
            response.getStatusCode(),
            HttpStatus.OK,
            Set.of(),
            () -> {
                assertThat(response.getBody()).as("health body").containsEntry("status", "UP");
                return null;
            },
            null,
            VoidVerification::new);
    }
}
