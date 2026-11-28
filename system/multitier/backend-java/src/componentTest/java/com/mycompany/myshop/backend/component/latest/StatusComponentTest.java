package com.mycompany.myshop.backend.component.latest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myshop.backend.BaseComponentTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.http.HttpStatus;

// The wake-up status endpoint, called with no token, against the WireMock-stubbed external systems.
class StatusComponentTest extends BaseComponentTest {

    private final TestRestTemplate client = new TestRestTemplate();

    private org.springframework.http.ResponseEntity<String> status(String system) {
        return client.getForEntity("http://localhost:" + port + "/api/status/" + system, String.class);
    }

    @Test
    void backendIsUp() {
        var response = status("backend");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"UP\"}");
    }

    @Test
    void erpIsUpWhenItsHealthAnswers() {
        ERP.stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(200)));

        assertThat(status("erp").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void erpIsDownWhenItsHealthFails() {
        ERP.stubFor(get(urlEqualTo("/health")).willReturn(aResponse().withStatus(503)));

        var response = status("erp");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).isEqualTo("{\"status\":\"DOWN\"}");
    }

    @Test
    void unknownSystemIsNotFound() {
        assertThat(status("bogus").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
