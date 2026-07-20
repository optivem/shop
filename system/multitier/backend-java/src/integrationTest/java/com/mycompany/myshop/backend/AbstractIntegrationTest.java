package com.mycompany.myshop.backend;

import com.mycompany.myshop.backend.backendtest.configuration.TestcontainersConfiguration;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
public abstract class AbstractIntegrationTest {
}
