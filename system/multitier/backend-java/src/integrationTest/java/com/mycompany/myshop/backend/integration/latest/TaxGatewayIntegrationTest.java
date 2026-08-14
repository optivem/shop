package com.mycompany.myshop.backend.integration.latest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myshop.backend.core.exceptions.TaxGatewayException;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.integration.latest.base.BaseGatewayIntegrationTest;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.external.tax.TaxDsl;
import org.junit.jupiter.api.Test;

/**
 * Narrow-integration coverage for {@link TaxGateway}, the sibling of
 * {@link ErpGatewayIntegrationTest}. Same shape: one gateway driven directly against the in-process
 * WireMock supplied by {@link BaseGatewayIntegrationTest}, with every stub declared through the
 * shared {@link TaxDsl} the component tests reach as {@code app.tax()}.
 *
 * <p>The 404 and 5xx cases are the ones worth pinning: {@code getTaxDetails} deliberately treats them
 * differently — a 404 is Tax answering "no such country" and yields an empty {@code Optional}, while
 * any other non-200 is Tax failing to answer and yields a {@link TaxGatewayException}. Collapsing the
 * two would turn an outage into a silent "country does not exist".
 *
 * <p>There is no {@code legacy/} counterpart, and that is a subset choice rather than an
 * impossibility — a raw-WireMock "before" for this gateway would be easy to write. It is skipped
 * because {@code legacy/} carries one twin per distinct contrast, not one per class (see
 * {@code component/legacy/CouponComponentTest}, whose twin exists to isolate the SUT-side contrast).
 * At this layer the sole contrast is inlined {@code WIRE_MOCK.stubFor} + JSON literal versus the
 * stub DSL, and {@code legacy/ErpGatewayIntegrationTest} already demonstrates it; a Tax twin would
 * repeat it verbatim against a different URL. Note this is a weaker claim than the one guarding
 * {@code contract/external/}, where a "before" would be genuinely circular.
 */
class TaxGatewayIntegrationTest extends BaseGatewayIntegrationTest {

    private final TaxGateway taxGateway = taxGateway();

    @Test
    void getTaxDetailsReturnsRateWhenCountryKnown() {
        tax().returnsTaxRate().country("US").taxRate("0.10").execute();

        var result = taxGateway.getTaxDetails("US");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("US");
        assertThat(result.get().getTaxRate()).isEqualByComparingTo("0.10");
    }

    @Test
    void getTaxDetailsReturnsEmptyWhenCountryUnknown() {
        tax().returnsNoTaxRate().country("ZZ").execute();

        assertThat(taxGateway.getTaxDetails("ZZ")).isEmpty();
    }

    @Test
    void getTaxDetailsThrowsOnServerError() {
        tax().failsForCountry().country("US").status(500).body("Internal Server Error").execute();

        assertThatThrownBy(() -> taxGateway.getTaxDetails("US"))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("500");
    }

    @Test
    void getTaxDetailsThrowsOnServiceUnavailable() {
        tax().failsForCountry().country("US").status(503).body("Service Unavailable").execute();

        assertThatThrownBy(() -> taxGateway.getTaxDetails("US"))
            .isInstanceOf(TaxGatewayException.class)
            .hasMessageContaining("503");
    }
}
