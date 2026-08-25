package com.mycompany.myshop.backend.usecases;

import com.mycompany.myshop.backend.common.Result;
import com.mycompany.myshop.backend.domain.exceptions.ValidationException;
import com.mycompany.myshop.backend.domain.gateways.ErpGatewayException;
import com.mycompany.myshop.backend.domain.rules.RuleViolation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// The guarantee the whole transport rule rests on: because every use case is wrapped in this, no use
// case has to catch anything, and none of them can forget to.
class RefusalTranslatingUseCaseTest {

    @Test
    void passesASuccessfulAnswerThrough() {
        var useCase = translating(request -> Result.ok("answered " + request));

        assertThat(useCase.execute("in").value()).isEqualTo("answered in");
    }

    @Test
    void passesAReturnedErrorThroughUntouched() {
        var expected = new UseCaseError.NotFound("Order", "ORD-404");
        var useCase = translating(request -> Result.err(expected));

        assertThat(useCase.execute("in").error()).isSameAs(expected);
    }

    @Test
    void turnsAThrownRefusalIntoTheSameErrorAReturnedOneWouldGive() {
        var useCase = translating(request -> {
            throw new ValidationException(new RuleViolation.LimitReached("couponCode", "used up"));
        });

        assertThat(useCase.execute("in").error())
                .isEqualTo(new UseCaseError.Invalid("couponCode", "used up"));
    }

    // A rule about the request as a whole names no field, and that has to survive the translation:
    // UseCaseResponder renders a null field as a bare 422 detail rather than an errors[] entry.
    @Test
    void keepsAFieldlessRefusalFieldless() {
        var useCase = translating(request -> {
            throw new ValidationException(new RuleViolation.NotAllowed("not today"));
        });

        assertThat(useCase.execute("in").error())
                .isEqualTo(new UseCaseError.Invalid(null, "not today"));
    }

    // The other half of the contract, and the more important one: a defect and an upstream outage
    // are not refusals, so they keep travelling. Catching them here is how a 500 quietly becomes a
    // 422 and a bug starts reporting itself to the caller as a business rule.
    @Test
    void letsADefectTravelOn() {
        var useCase = translating(request -> {
            throw new IllegalArgumentException("orderNumber cannot be null");
        });

        assertThatThrownBy(() -> useCase.execute("in"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void letsAGatewayFailureTravelOn() {
        var useCase = translating(request -> {
            throw new ErpGatewayException("ERP did not answer");
        });

        assertThatThrownBy(() -> useCase.execute("in"))
                .isInstanceOf(ErpGatewayException.class);
    }

    private static UseCase<String, String> translating(UseCase<String, String> delegate) {
        return new RefusalTranslatingUseCase<>(delegate);
    }
}
