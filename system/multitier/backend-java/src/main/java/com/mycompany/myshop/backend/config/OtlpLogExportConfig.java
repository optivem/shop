package com.mycompany.myshop.backend.config;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Spring Boot builds the OTLP log exporter but does not connect Logback to it. This adds the OTel
// appender next to the console one -- stdout JSON is untouched -- and only when OTLP export is on,
// i.e. when OtlpExportEnvironmentPostProcessor has set the logging endpoint.
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("management.otlp.logging.endpoint")
public class OtlpLogExportConfig {

    @Bean
    InitializingBean otlpLogbackAppenderInstaller(OpenTelemetry openTelemetry) {
        return () -> {
            var context = (LoggerContext) LoggerFactory.getILoggerFactory();
            var appender = new OpenTelemetryAppender();
            appender.setContext(context);
            appender.setName("OTEL");
            appender.start();
            context.getLogger(Logger.ROOT_LOGGER_NAME).addAppender(appender);
            OpenTelemetryAppender.install(openTelemetry);
        };
    }
}
