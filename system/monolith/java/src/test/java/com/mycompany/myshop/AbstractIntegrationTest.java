package com.mycompany.myshop;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class AbstractIntegrationTest {

	@Container
	@ServiceConnection
	static final PostgreSQLContainer POSTGRES =
		new PostgreSQLContainer("postgres:16-alpine")
			.withDatabaseName("app")
			.withUsername("app")
			.withPassword("app");
}
