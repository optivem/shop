package com.mycompany.myshop.backend.core.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StatusServiceTest {

    private HttpServer external;
    private final AtomicInteger calls = new AtomicInteger();
    private volatile int status = 200;
    private String baseUrl;
    private DataSource dataSource;

    @BeforeEach
    void start() throws IOException {
        external = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        external.createContext("/", exchange -> {
            calls.incrementAndGet();
            exchange.sendResponseHeaders(exchange.getRequestURI().getPath().endsWith("/health") ? status : 404, -1);
            exchange.close();
        });
        external.start();
        baseUrl = "http://127.0.0.1:" + external.getAddress().getPort();
        dataSource = mock(DataSource.class);
    }

    @AfterEach
    void stop() {
        external.stop(0);
    }

    private StatusService service(String mode) {
        return new StatusService(dataSource, mode, baseUrl + "/erp", baseUrl + "/tax", baseUrl + "/clock");
    }

    @Test
    void backendIsUpWhenTheDatabaseAnswers() throws SQLException {
        var connection = mock(Connection.class);
        when(connection.isValid(5)).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(connection);

        assertThat(service("stub").isUp("backend")).contains(true);
    }

    @Test
    void backendIsDownWhenTheDatabaseDoesNotAnswer() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("refused"));

        assertThat(service("stub").isUp("backend")).contains(false);
    }

    @Test
    void erpIsUpOn200() {
        assertThat(service("stub").isUp("erp")).contains(true);
    }

    @Test
    void erpIsDownOn503WithoutRetry() {
        status = 503;

        assertThat(service("stub").isUp("erp")).contains(false);
        assertThat(calls).hasValue(1);
    }

    @Test
    void erpIsDownWhenConnectionRefused() {
        external.stop(0);

        assertThat(service("stub").isUp("erp")).contains(false);
    }

    @Test
    void taxIsUpOn200() {
        assertThat(service("stub").isUp("tax")).contains(true);
    }

    @Test
    void clockIsUpInRealModeWithoutACall() {
        assertThat(service("real").isUp("clock")).contains(true);
        assertThat(calls).hasValue(0);
    }

    @Test
    void clockIsProbedInStubMode() {
        status = 503;

        assertThat(service("stub").isUp("clock")).contains(false);
        assertThat(calls).hasValue(1);
    }

    @Test
    void unknownSystemIsEmpty() {
        assertThat(service("stub").isUp("bogus")).isEqualTo(Optional.empty());
    }
}
