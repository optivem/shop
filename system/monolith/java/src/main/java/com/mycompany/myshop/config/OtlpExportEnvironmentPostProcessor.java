package com.mycompany.myshop.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

// OTLP export is opt-in: it switches on only when OTEL_EXPORTER_OTLP_ENDPOINT is set and non-empty
// (e.g. by the observability Compose profile). Spring Boot does not read that variable itself, and an
// empty management.otlp.tracing.endpoint would still create an exporter, so the mapping is done here.
// Without the variable, spans are still created (they put trace_id/span_id into the logs) but nothing
// is exported and no connection is ever attempted.
public class OtlpExportEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String ENDPOINT_VARIABLE = "OTEL_EXPORTER_OTLP_ENDPOINT";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        var endpoint = environment.getProperty(ENDPOINT_VARIABLE, "").strip();
        Map<String, Object> properties = new HashMap<>();
        if (endpoint.isEmpty()) {
            properties.put("management.otlp.metrics.export.enabled", false);
        } else {
            var base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
            properties.put("management.otlp.tracing.endpoint", base + "/v1/traces");
            properties.put("management.otlp.metrics.export.url", base + "/v1/metrics");
            properties.put("management.otlp.logging.endpoint", base + "/v1/logs");
        }
        // Lowest precedence, so an explicit management.otlp.* setting still wins.
        environment.getPropertySources().addLast(new MapPropertySource("otlpExportFromOtelEnv", properties));
    }
}
