package com.mycompany.myshop.backend;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.core.repositories.CouponRepository;
import com.mycompany.myshop.backend.core.repositories.OrderRepository;
import com.mycompany.myshop.backend.core.services.external.ClockGateway;
import com.mycompany.myshop.backend.core.services.external.ErpGateway;
import com.mycompany.myshop.backend.core.services.external.TaxGateway;
import com.mycompany.myshop.backend.support.harness.BackendDriver;
import com.mycompany.myshop.backend.support.harness.ExternalSystemMode;
import com.mycompany.myshop.backend.support.harness.StubDrivers;
import com.mycompany.myshop.backend.support.harness.SutClockReader;
import com.mycompany.myshop.backend.support.harness.SutErpReader;
import com.mycompany.myshop.backend.support.harness.SutTaxReader;
import com.mycompany.myshop.backend.support.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.support.core.usecase.UseCaseDsl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * In-process component-test harness: boots the Spring app on a random port (real HTTP over a real
 * socket), backs it with a Testcontainers-managed Postgres (real dialect, Flyway-migrated) and
 * stubs the ERP / Tax / Clock external HTTP systems with in-process WireMock. No docker compose,
 * no deployment. The same harness is reused by the Pact provider-verification test.
 *
 * <p>Postgres is supplied by a {@code @ServiceConnection} bean ({@link TestcontainersConfiguration},
 * shared with the narrow-integration layer): the container's lifecycle is tied to the Spring
 * context, which is cached and shared across every subclass and the Pact verifier, so it stays up
 * for as long as any test can reach it. WireMock uses the singleton-container pattern instead
 * (started once in a static initializer, never stopped by JUnit) rather than {@code @Container}/
 * {@code @Testcontainers}: it is not a Spring bean, so a per-class server that JUnit stopped after
 * the first class would leave the reused, cached context pointing at a dead port.
 *
 * <p>Tests reach the harness through two layers: {@link #scenario} — {@code given() / when() /
 * then()}, what {@code latest/} tests are written on — and {@link #app}, the use case layer
 * underneath it, for a test that needs surgical control.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class AbstractComponentTest {

    protected static final WireMockServer ERP = new WireMockServer(options().dynamicPort());
    protected static final WireMockServer TAX = new WireMockServer(options().dynamicPort());
    protected static final WireMockServer CLOCK = new WireMockServer(options().dynamicPort());

    static {
        ERP.start();
        TAX.start();
        CLOCK.start();
    }

    @DynamicPropertySource
    static void externalSystemProperties(DynamicPropertyRegistry registry) {
        // Drive the ClockGateway through HTTP (rather than Instant.now()) so time is controllable.
        registry.add("external.system-mode", ExternalSystemMode.STUB::propertyValue);
        registry.add("erp.url", ERP::baseUrl);
        registry.add("tax.url", TAX::baseUrl);
        registry.add("clock.url", CLOCK::baseUrl);
    }

    @LocalServerPort
    protected int port;

    @Autowired
    protected TestRestTemplate restTemplate;

    /**
     * The application's own Jackson mapper, handed to the use case layer so it parses responses
     * exactly the way the app serializes them — including the {@code ProblemDetail} support Spring
     * Boot registers, which the DSL needs to read a rejection's {@code detail} / {@code errors[]}.
     */
    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected CouponRepository couponRepository;

    /**
     * The SUT's production gateways to the external systems. The stub-contract tests read them back
     * through {@link SutErpReader} / {@link SutTaxReader} / {@link SutClockReader} so the WireMock
     * stub's bytes travel through the SUT's real HTTP call + real parse, not a test-side client.
     */
    @Autowired
    protected ErpGateway erpGateway;

    @Autowired
    protected TaxGateway taxGateway;

    @Autowired
    protected ClockGateway clockGateway;

    /**
     * The use case layer: one entry per actor — {@code app.myShop()} for the system under test,
     * {@code app.erp()} / {@code app.tax()} / {@code app.clock()} for the external stubs. The
     * {@link #scenario} DSL is built on it, and it stays exposed for tests that need to drive an
     * actor directly rather than through a scenario.
     */
    protected UseCaseDsl app;

    /**
     * The scenario layer: {@code scenario.given()…when()…then()}. Fresh per test — the
     * one-scenario-per-test guard lives on the instance, so a new test starts with a clean slate.
     */
    protected ScenarioDslImpl scenario;

    @BeforeEach
    void resetComponentState() {
        ERP.resetAll();
        TAX.resetAll();
        CLOCK.resetAll();
        orderRepository.deleteAll();
        couponRepository.deleteAll();

        // Wired here rather than as field initializers: restTemplate/objectMapper are autowired
        // instance fields, not yet populated at field-init time.
        app = new UseCaseDsl(
            new BackendDriver(restTemplate),
            objectMapper,
            StubDrivers.erp(ERP),
            StubDrivers.tax(TAX),
            StubDrivers.clock(CLOCK),
            new SutErpReader(erpGateway),
            new SutTaxReader(taxGateway),
            new SutClockReader(clockGateway));
        scenario = new ScenarioDslImpl(app);
    }
}
