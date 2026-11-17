package com.mycompany.myshop.testkit.dsl.core.usecase.usecases;

import com.mycompany.myshop.testkit.driver.port.dtos.CheckAccessibilityResponse;
import com.mycompany.myshop.testkit.dsl.core.shared.ResponseVerification;
import com.mycompany.myshop.testkit.dsl.core.shared.UseCaseContext;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the accessibility check of the page that is open. */
public class CheckAccessibilityVerification extends ResponseVerification<CheckAccessibilityResponse> {
    public CheckAccessibilityVerification(CheckAccessibilityResponse response, UseCaseContext context) {
        super(response, context);
    }

    /** axe found no serious or critical violation. */
    public CheckAccessibilityVerification noViolations() {
        var violations = getResponse().getViolations();
        assertThat(violations)
                .withFailMessage("Expected no serious or critical axe violations on the page, but found: %s", violations)
                .isEmpty();
        return this;
    }
}
