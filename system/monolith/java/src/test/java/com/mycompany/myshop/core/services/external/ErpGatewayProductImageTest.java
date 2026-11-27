package com.mycompany.myshop.core.services.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mycompany.myshop.core.exceptions.ErpUnavailableException;
import io.micrometer.observation.ObservationRegistry;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The ERP gateway's product image fetch, with the ERP scripted at the HTTP boundary: the ERP's image
 * path is resolved against the ERP base URL and the bytes come back with their content type.
 */
class ErpGatewayProductImageTest {

    private static final byte[] IMAGE = {1, 2, 3};

    private HttpServer erp;
    private final AtomicReference<String> lastPath = new AtomicReference<>();
    private volatile int status;
    private ErpGateway erpGateway;

    @BeforeEach
    void startErp() throws IOException {
        erp = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        erp.createContext("/", exchange -> {
            lastPath.set(exchange.getRequestURI().getPath());
            var ok = status == 200;
            exchange.getResponseHeaders().set("Content-Type", ok ? "image/jpeg" : "text/plain");
            var body = ok ? IMAGE : new byte[0];
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (var out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        erp.start();
        var fastRetries = new ResilienceSettings(Duration.ofSeconds(1), Duration.ofSeconds(1),
                new ResilienceSettings.RetrySettings(1, Duration.ofMillis(1), 2.0, 0.5),
                ResilienceSettings.DEFAULTS.circuitBreaker());
        erpGateway = new ErpGateway();
        ReflectionTestUtils.setField(erpGateway, "erpUrl", "http://127.0.0.1:" + erp.getAddress().getPort() + "/erp");
        ReflectionTestUtils.setField(erpGateway, "observationRegistry", ObservationRegistry.NOOP);
        ReflectionTestUtils.setField(erpGateway, "resilience", fastRetries);
        ReflectionTestUtils.invokeMethod(erpGateway, "initHttpClient");
    }

    @AfterEach
    void stopErp() {
        erp.stop(0);
    }

    @Test
    void fetchesTheImageFromThePathTheErpGaveWithItsContentType() {
        status = 200;

        var image = erpGateway.getProductImage("/erp/images/HP-15.jpg");

        assertThat(lastPath.get()).isEqualTo("/erp/images/HP-15.jpg");
        assertThat(image).hasValueSatisfying(found -> {
            assertThat(found.contentType()).isEqualTo("image/jpeg");
            assertThat(found.content()).containsExactly(IMAGE);
        });
    }

    @Test
    void isEmptyWhenTheErpHasNoSuchImage() {
        status = 404;

        assertThat(erpGateway.getProductImage("/erp/images/UNKNOWN.jpg")).isEmpty();
    }

    @Test
    void throwsErpUnavailableOnServerError() {
        status = 500;

        assertThatThrownBy(() -> erpGateway.getProductImage("/erp/images/HP-15.jpg"))
                .isInstanceOf(ErpUnavailableException.class)
                .hasMessageContaining("500");
    }
}
