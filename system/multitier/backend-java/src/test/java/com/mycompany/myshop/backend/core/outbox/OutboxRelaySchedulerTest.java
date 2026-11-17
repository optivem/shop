package com.mycompany.myshop.backend.core.outbox;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** The relay can be switched off by configuration; the defaults bind with nothing configured. */
class OutboxRelaySchedulerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(RelayConfiguration.class, OutboxRelayScheduler.class);

    @Test
    void schedulesTheRelayByDefaultWithTheDefaultSettings() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(OutboxRelayScheduler.class);
            var settings = context.getBean(OutboxRelaySettings.class);
            assertThat(settings.enabled()).isTrue();
            assertThat(settings.pollInterval()).isEqualTo(Duration.ofSeconds(2));
            assertThat(settings.batchSize()).isEqualTo(50);
            assertThat(settings.maxAttempts()).isEqualTo(5);
            assertThat(settings.initialBackoff()).isEqualTo(Duration.ofSeconds(1));
        });
    }

    @Test
    void schedulesNothingWhenTheRelayIsSwitchedOff() {
        contextRunner.withPropertyValues("outbox.relay.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(OutboxRelayScheduler.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(OutboxRelaySettings.class)
    static class RelayConfiguration {
        @Bean
        OutboxRelay outboxRelay() {
            return mock(OutboxRelay.class);
        }
    }
}
