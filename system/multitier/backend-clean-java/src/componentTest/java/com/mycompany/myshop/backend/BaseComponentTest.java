package com.mycompany.myshop.backend;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.CouponJpaRepository;
import com.mycompany.myshop.backend.infrastructure.persistence.repositories.OrderJpaRepository;
import com.mycompany.myshop.backend.domain.gateways.ClockGateway;
import com.mycompany.myshop.backend.domain.gateways.ErpGateway;
import com.mycompany.myshop.backend.domain.gateways.TaxGateway;
import com.mycompany.myshop.backend.testkit.driver.adapter.api.BackendDriver;
import com.mycompany.myshop.backend.backendtest.configuration.ExternalSystemMode;
import com.mycompany.myshop.backend.backendtest.configuration.StubDrivers;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutClockReader;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutErpReader;
import com.mycompany.myshop.backend.testkit.driver.adapter.sut.SutTaxReader;
import com.mycompany.myshop.backend.testkit.dsl.core.ScenarioDslImpl;
import com.mycompany.myshop.backend.testkit.dsl.core.usecase.UseCaseDsl;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
public abstract class BaseComponentTest {

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

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected OrderJpaRepository orderRepository;

    @Autowired
    protected CouponJpaRepository couponRepository;

    @Autowired
    protected ErpGateway erpGateway;

    @Autowired
    protected TaxGateway taxGateway;

    @Autowired
    protected ClockGateway clockGateway;

    protected UseCaseDsl app;

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
